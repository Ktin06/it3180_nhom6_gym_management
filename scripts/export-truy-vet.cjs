#!/usr/bin/env node
// Sinh ma trận truy vết Chức năng → UC → US → AC → TC (ADR 0012).
// Đọc mã UC, US, AC, BR trong docs/requirements/srs/ và mã TC trong tên kiểm thử
// (JUnit @DisplayName, Vitest it/test/describe), rồi ghi docs/testing/ma-tran-truy-vet.md.
//
// Chạy ở gốc repo:
//   node scripts/export-truy-vet.cjs           # sinh lại file ma trận
//   node scripts/export-truy-vet.cjs --check   # chỉ kiểm tra; lỗi (TC trỏ AC không tồn tại) thì thoát mã 1
const fs = require('node:fs');
const path = require('node:path');

const root = path.resolve(__dirname, '..');
const SRS_DIR = path.join(root, 'docs/requirements/srs');
const OUT = path.join(root, 'docs/testing/ma-tran-truy-vet.md');
const TEST_DIRS = [
  { dir: path.join(root, 'backend/src/test/java'), kind: 'java' },
  { dir: path.join(root, 'frontend/src'), kind: 'vitest' },
];
const checkOnly = process.argv.includes('--check');

const TC_RE = /TC-F(\d+)\.(\d+)\.(\d+)-(\d\d)([a-z])/g;
const rel = (p) => path.relative(root, p).split(path.sep).join('/');
const fNum = (code) => Number(/F(\d+)/.exec(code)[1]);
const byCode = (a, b) => a.localeCompare(b, 'en', { numeric: true });

function walk(dir, accept, out = []) {
  if (!fs.existsSync(dir)) return out;
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    if (entry.name === 'node_modules' || entry.name.startsWith('.')) continue;
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) walk(full, accept, out);
    else if (accept(entry.name)) out.push(full);
  }
  return out;
}

// ---------------------------------------------------------------- SRS
function readSrs() {
  const ucs = new Map(); // UC-F3.1 -> {code, name, file, uss: []}
  const uss = new Map(); // US-F3.1.1 -> {code, name, uc, br, issue, acs: []}
  const acs = new Map(); // AC-F3.1.1-01 -> {code, name, us, br, dropped}
  const files = fs.readdirSync(SRS_DIR).filter((f) => /^dac-ta-use-case-f\d+-.*\.md$/.test(f));
  for (const file of files) {
    const lines = fs.readFileSync(path.join(SRS_DIR, file), 'utf8').replace(/\r\n/g, '\n').split('\n');
    let uc = null;
    let us = null;
    for (const line of lines) {
      let m = /^###\s+[\d.]*\s*(UC-F\d+\.\d+):\s*(.+?)\s*$/.exec(line);
      if (m) {
        uc = { code: m[1], name: m[2], file: `docs/requirements/srs/${file}`, uss: [] };
        ucs.set(uc.code, uc);
        us = null;
        continue;
      }
      m = /^#{4,6}\s+(US-F\d+\.\d+\.\d+):\s*(.+?)\s*$/.exec(line);
      if (m) {
        us = { code: m[1], name: m[2], uc: uc ? uc.code : null, br: [], issue: '', acs: [] };
        uss.set(us.code, us);
        if (uc) uc.uss.push(us.code);
        continue;
      }
      if (us) {
        m = /^- \*\*BR liên quan:\*\*\s*(.*)$/.exec(line);
        if (m) us.br = [...m[1].matchAll(/BR-F\d+[-.][\d.-]+\d/g)].map((x) => x[0]);
        m = /^- \*\*Issue:\*\*\s*(.*)$/.exec(line);
        if (m) us.issue = m[1].trim();
      }
      m = /^\s*- \*\*(AC-F\d+\.\d+\.\d+-\d\d):\s*(.+?)\*\*(.*)$/.exec(line);
      if (m) {
        const owner = m[1].replace(/^AC-(F[\d.]+)-\d\d$/, 'US-$1');
        const ac = {
          code: m[1],
          name: m[2].trim(),
          us: owner,
          br: [...m[3].matchAll(/BR-F\d+[-.][\d.-]+\d/g)].map((x) => x[0]),
          dropped: /\(bỏ ngày/.test(m[2] + m[3]),
        };
        if (acs.has(ac.code)) ac.duplicate = true;
        acs.set(ac.code, ac);
        if (uss.has(owner)) uss.get(owner).acs.push(ac.code);
      }
    }
  }
  return { ucs, uss, acs };
}

// ---------------------------------------------------------------- Kiểm thử
function readTests() {
  const tcs = []; // {code, ac, label, where}
  const untagged = new Map(); // file -> số kiểm thử không mang mã TC
  const tag = (file, label, where) => {
    const codes = [...label.matchAll(TC_RE)].map((m) => m[0]);
    if (codes.length === 0) {
      untagged.set(file, (untagged.get(file) || 0) + 1);
      return;
    }
    for (const code of codes) {
      tcs.push({ code, ac: code.replace(/^TC-/, 'AC-').replace(/[a-z]$/, ''), label, where });
    }
  };
  for (const { dir, kind } of TEST_DIRS) {
    const files = walk(dir, (n) => (kind === 'java' ? n.endsWith('Test.java') : /\.test\.tsx?$/.test(n)));
    for (const file of files) {
      const text = fs.readFileSync(file, 'utf8').replace(/\r\n/g, '\n');
      const r = rel(file);
      if (kind === 'java') {
        const cls = path.basename(file, '.java');
        // Mỗi phương thức @Test/@ParameterizedTest/@RepeatedTest; lấy @DisplayName ngay trên nó nếu có.
        const re = /((?:\s*@[A-Za-z]+(?:\((?:[^()]|\([^()]*\))*\))?)+)\s*(?:public\s+|protected\s+|private\s+)?void\s+(\w+)\s*\(/g;
        let m;
        while ((m = re.exec(text))) {
          const annotations = m[1];
          if (!/@(Test|ParameterizedTest|RepeatedTest)\b/.test(annotations)) continue;
          const dn = /@DisplayName\(\s*"((?:[^"\\]|\\.)*)"\s*\)/.exec(annotations);
          tag(r, dn ? dn[1] : '', `${cls}#${m[2]}`);
        }
      } else {
        const re = /\b(it|test|describe)\(\s*(['"`])((?:\\.|(?!\2).)*)\2/g;
        let m;
        while ((m = re.exec(text))) {
          if (m[1] === 'describe' && !TC_RE.test(m[3])) { TC_RE.lastIndex = 0; continue; }
          TC_RE.lastIndex = 0;
          tag(r, m[3], `${r.replace(/^frontend\/src\//, '')} › ${m[3].slice(0, 60)}`);
        }
      }
    }
  }
  return { tcs, untagged };
}

// ---------------------------------------------------------------- Xuất báo cáo
function build() {
  const { ucs, uss, acs } = readSrs();
  const { tcs, untagged } = readTests();
  const tcByAc = new Map();
  for (const tc of tcs) {
    if (!tcByAc.has(tc.ac)) tcByAc.set(tc.ac, []);
    tcByAc.get(tc.ac).push(tc);
  }
  const live = [...acs.values()].filter((a) => !a.dropped);
  const missing = live.filter((a) => !tcByAc.has(a.code)).sort((a, b) => byCode(a.code, b.code));
  const orphans = tcs.filter((t) => !acs.has(t.ac) || acs.get(t.ac).dropped).sort((a, b) => byCode(a.code, b.code));
  const duplicates = [...acs.values()].filter((a) => a.duplicate).map((a) => a.code);
  const usWithoutUc = [...uss.values()].filter((u) => !u.uc || !ucs.has(u.uc)).map((u) => u.code);

  const L = [];
  const p = (s = '') => L.push(s);
  p('# Ma trận truy vết UC → US → AC → TC');
  p();
  p('> **File sinh tự động, không sửa tay.** Chạy `node scripts/export-truy-vet.cjs` ở gốc repo để sinh lại.');
  p('> Nguồn: mã UC, US, AC, BR trong `docs/requirements/srs/`; mã TC trong tên kiểm thử backend (JUnit `@DisplayName`)');
  p('> và frontend (Vitest `it`, `test`, `describe`). Quy ước và cách dùng: [README](README.md), [ADR 0012](../decisions/0012-quy-uoc-ma-truy-vet.md).');
  p();

  // 1. Tổng quan
  p('## 1. Tổng quan theo chức năng');
  p();
  p('| Chức năng | UC | US | AC | AC đã có TC | AC chưa có TC | TC |');
  p('| --- | --- | --- | --- | --- | --- | --- |');
  const fs_ = [...new Set([...ucs.keys()].map(fNum))].sort((a, b) => a - b);
  let tot = { uc: 0, us: 0, ac: 0, ok: 0, tc: 0 };
  for (const f of fs_) {
    const fUc = [...ucs.values()].filter((u) => fNum(u.code) === f);
    const fUs = [...uss.values()].filter((u) => fNum(u.code) === f);
    const fAc = live.filter((a) => fNum(a.code) === f);
    const ok = fAc.filter((a) => tcByAc.has(a.code)).length;
    const nTc = tcs.filter((t) => fNum(t.code) === f && acs.has(t.ac)).length;
    tot = { uc: tot.uc + fUc.length, us: tot.us + fUs.length, ac: tot.ac + fAc.length, ok: tot.ok + ok, tc: tot.tc + nTc };
    p(`| F${f} | ${fUc.length} | ${fUs.length} | ${fAc.length} | ${ok} | ${fAc.length - ok} | ${nTc} |`);
  }
  p(`| **Tổng** | **${tot.uc}** | **${tot.us}** | **${tot.ac}** | **${tot.ok}** | **${tot.ac - tot.ok}** | **${tot.tc}** |`);
  p();
  p('Chức năng có US = 0 là chức năng chưa viết lại SRS theo ADR 0012 (chưa có mã US, AC).');
  p();

  // 2. Lỗi cần sửa
  p('## 2. Lỗi cần sửa');
  p();
  p('### 2.1. TC trỏ tới AC không tồn tại hoặc đã bỏ');
  p();
  if (orphans.length === 0) p('Không có.');
  else {
    p('| TC | AC không tìm thấy | Kiểm thử |');
    p('| --- | --- | --- |');
    for (const t of orphans) p(`| ${t.code} | ${t.ac} | \`${t.where}\` |`);
  }
  p();
  p('### 2.2. Mã AC bị trùng trong SRS');
  p();
  p(duplicates.length ? duplicates.map((c) => `- ${c}`).join('\n') : 'Không có.');
  p();
  p('### 2.3. US không nằm dưới UC nào');
  p();
  p(usWithoutUc.length ? usWithoutUc.map((c) => `- ${c}`).join('\n') : 'Không có.');
  p();

  // 3. AC chưa có TC
  p('## 3. AC chưa có TC');
  p();
  p('Một US chỉ đủ điều kiện nghiệm thu và đóng khi không còn AC nào của nó trong bảng này ([CONTRIBUTING.md, mục 5](../../CONTRIBUTING.md#5-định-nghĩa-hoàn-thành)).');
  p();
  if (missing.length === 0) p('Không có.');
  else {
    p('| AC | Tên | US |');
    p('| --- | --- | --- |');
    for (const a of missing) p(`| ${a.code} | ${a.name} | ${a.us} |`);
  }
  p();

  // 4. Cây xuôi
  p('## 4. Cây xuôi: chức năng → UC → US → AC → TC');
  p();
  for (const f of fs_) {
    const fUc = [...ucs.values()].filter((u) => fNum(u.code) === f).sort((a, b) => byCode(a.code, b.code));
    const fUs = fUc.flatMap((u) => u.uss);
    if (fUs.length === 0) continue;
    p(`### F${f}`);
    p();
    for (const uc of fUc) {
      p(`- **${uc.code}** ${uc.name}`);
      for (const usCode of uc.uss) {
        const us = uss.get(usCode);
        const done = us.acs.filter((c) => !acs.get(c).dropped).every((c) => tcByAc.has(c));
        p(`  - **${us.code}** ${us.name}${us.issue ? ` (${us.issue})` : ''}${done && us.acs.length ? ' ✅ mọi AC có TC' : ''}`);
        for (const acCode of us.acs) {
          const ac = acs.get(acCode);
          const list = tcByAc.get(acCode) || [];
          const mark = ac.dropped ? ' _(đã bỏ)_' : list.length ? '' : ' ❌ chưa có TC';
          p(`    - ${ac.code} ${ac.name}${ac.br.length ? ` (${ac.br.join(', ')})` : ''}${mark}`);
          for (const t of list.sort((a, b) => byCode(a.code, b.code))) p(`      - ${t.code} \`${t.where}\``);
        }
      }
    }
    p();
  }

  // 5. Bảng ngược
  p('## 5. Bảng ngược: TC → AC → US → UC → BR');
  p();
  p('Dùng khi một kiểm thử báo lỗi: tìm mã TC để biết nó vi phạm AC, US và quy tắc nghiệp vụ nào.');
  p();
  p('| TC | AC | US | UC | BR | Kiểm thử |');
  p('| --- | --- | --- | --- | --- | --- |');
  for (const t of tcs.filter((x) => acs.has(x.ac)).sort((a, b) => byCode(a.code, b.code))) {
    const ac = acs.get(t.ac);
    const us = uss.get(ac.us);
    const br = [...new Set([...(ac.br || []), ...((us && us.br) || [])])].join(', ');
    p(`| ${t.code} | ${ac.code} | ${ac.us} | ${us ? us.uc : ''} | ${br} | \`${t.where}\` |`);
  }
  p();

  // 6. Kiểm thử chưa gắn mã
  const untaggedTotal = [...untagged.values()].reduce((a, b) => a + b, 0);
  p('## 6. Kiểm thử chưa mang mã TC');
  p();
  p(`Có ${untaggedTotal} kiểm thử chưa mang mã TC. Gồm kiểm thử thuần kỹ thuật (JWT, cấu hình, ràng buộc database; được phép ghi mã \`S…\` hay \`BR-…\` thay mã TC) và kiểm thử của chức năng chưa gắn mã.`);
  p();
  p('| File | Số kiểm thử |');
  p('| --- | --- |');
  for (const [file, n] of [...untagged.entries()].sort((a, b) => byCode(a[0], b[0]))) p(`| \`${file}\` | ${n} |`);
  p();

  return {
    text: L.join('\n'),
    errors: orphans.length + duplicates.length + usWithoutUc.length,
    summary: `${tot.uc} UC, ${tot.us} US, ${tot.ac} AC (${tot.ok} có TC, ${tot.ac - tot.ok} chưa có), ${tot.tc} TC, ${untaggedTotal} kiểm thử chưa gắn mã; lỗi: ${orphans.length} TC trỏ sai, ${duplicates.length} AC trùng, ${usWithoutUc.length} US lạc`,
  };
}

const result = build();
console.log(result.summary);
if (!checkOnly) {
  fs.writeFileSync(OUT, result.text, 'utf8');
  console.log('Đã ghi', rel(OUT));
}
if (result.errors > 0) process.exitCode = 1;
