#!/usr/bin/env node
// Sinh từ điển từ mô hình của bộ phân tích DBML chính thức; không sửa schema.
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');

const root = path.resolve(__dirname, '..');
const args = process.argv.slice(2);
const coreIndex = args.indexOf('--dbml-core');
if (coreIndex < 0 || !args[coreIndex + 1]) {
  console.error('Dùng: node scripts/export-data-dictionary.cjs --dbml-core <đường dẫn @dbml/core> [--check]');
  process.exit(1);
}
const { Parser, ModelExporter } = require(path.resolve(args[coreIndex + 1]));
const sourcePath = path.join(root, 'docs/design/erd/erd-v2.dbml');
const outputPath = path.join(root, 'docs/design/erd/tu-dien-du-lieu.md');
const source = fs.readFileSync(sourcePath, 'utf8').replace(/\r\n/g, '\n');
const database = new Parser().parse(source, 'dbmlv2');
const schema = JSON.parse(ModelExporter.export(database, 'json', { isNormalized: false })).schemas[0];
const tables = schema.tables;
const enums = new Map(schema.enums.map(e => [e.name, e]));
const checksum = crypto.createHash('sha256').update(source).digest('hex');

const escape = value => String(value).replace(/&/g, '&amp;').replace(/</g, '&lt;')
  .replace(/>/g, '&gt;').replace(/\|/g, '&#124;').replace(/\r?\n/g, '<br>');
const code = value => '`' + String(value).replace(/`/g, '&#96;').replace(/\|/g, '&#124;') + '`';

// Tên nhóm và vị trí nguồn được đọc từ chính các mục của DBML.
const groups = [];
const locations = new Map();
const foreignKeys = new Map();
let currentGroup;
let currentTable;
for (const [i, line] of source.split('\n').entries()) {
  const group = line.match(/^\/\/ (\d+)\. (.+?)(?: \(\d+ bảng\))?(?: –.*)?$/u);
  if (group) {
    currentGroup = { number: group[1], title: group[2], tables: [] };
    groups.push(currentGroup);
  }
  const table = line.match(/^Table (\w+) \{/);
  if (table) {
    currentTable = table[1];
    currentGroup.tables.push(currentTable);
    locations.set(currentTable, i + 1);
  }
  const reference = line.match(/^\s+(\w+)\s+.+?\[.*?ref:\s*([><-])\s*(\w+)\.(\w+)/);
  if (reference) foreignKeys.set(`${currentTable}.${reference[1]}`, {
    table: reference[3], field: reference[4], relation: reference[2],
  });
}
if (foreignKeys.size !== schema.refs.length) throw new Error('Không trích đủ khóa ngoại trong DBML.');

// Ý nghĩa bảng giữ theo tài liệu giải thích dữ liệu có sẵn.
const purposes = new Map();
const designText = fs.readFileSync(path.join(root, 'docs/design/erd/thiet-ke-du-lieu.md'), 'utf8');
for (const match of designText.matchAll(/^\| `(\w+)` \| ([^|]+) \|/gm)) {
  if (!purposes.has(match[1])) purposes.set(match[1], match[2].trim());
}

// Chỉ diễn giải tên trường khi nguồn không có ghi chú; không bổ sung ràng buộc.
const labels = {
  id: 'Khóa chính của bản ghi', email: 'Địa chỉ email', password_hash: 'Mã băm mật khẩu',
  role: 'Vai trò tài khoản', status: 'Trạng thái bản ghi', type: 'Loại bản ghi',
  failed_login_count: 'Số lần đăng nhập sai liên tiếp', locked_until: 'Thời điểm kết thúc khóa đăng nhập tạm',
  last_login_at: 'Thời điểm đăng nhập gần nhất', created_at: 'Thời điểm tạo bản ghi (UTC)',
  updated_at: 'Thời điểm cập nhật bản ghi (UTC)', token_hash: 'Mã băm token',
  expires_at: 'Thời điểm hết hạn', revoked_at: 'Thời điểm thu hồi token', used_at: 'Thời điểm sử dụng token',
  action: 'Loại thao tác được ghi nhận', entity: 'Tên bảng được tác động', entity_id: 'Định danh bản ghi được tác động',
  old_value: 'Dữ liệu trước thay đổi', new_value: 'Dữ liệu sau thay đổi', ip: 'Địa chỉ IP của thao tác',
  title: 'Tiêu đề', content: 'Nội dung', channel: 'Kênh gửi thông báo',
  related_entity: 'Loại đối tượng liên quan', related_id: 'Định danh đối tượng liên quan',
  scheduled_at: 'Thời điểm dự kiến gửi', sent_at: 'Thời điểm đã gửi', read_at: 'Thời điểm đã đọc',
  config_key: 'Khóa cấu hình', config_value: 'Giá trị cấu hình', description: 'Mô tả',
  code: 'Mã định danh nghiệp vụ', name: 'Tên', address: 'Địa chỉ', phone: 'Số điện thoại',
  open_time: 'Giờ mở cửa', close_time: 'Giờ đóng cửa', capacity: 'Sức chứa hoặc sĩ số tối đa',
  purchased_at: 'Ngày mua thiết bị', issue: 'Nội dung báo hỏng', scheduled_date: 'Ngày dự kiến bảo trì',
  completed_date: 'Ngày hoàn thành bảo trì', cost: 'Chi phí bảo trì (VND)',
  member_code: 'Mã hội viên', full_name: 'Họ và tên', gender: 'Giới tính', dob: 'Ngày sinh',
  avatar_url: 'Đường dẫn ảnh đại diện', face_consent_at: 'Thời điểm đồng ý sử dụng khuôn mặt',
  booking_locked_until: 'Thời điểm kết thúc khóa đặt lớp', deleted_at: 'Thời điểm xóa mềm',
  embedding: 'Vector đặc trưng khuôn mặt', model_version: 'Phiên bản mô hình nhận diện',
  measured_at: 'Ngày đo chỉ số cơ thể', weight: 'Khối lượng (kg)', height: 'Chiều cao (cm)',
  body_fat: 'Tỷ lệ mỡ cơ thể (%)', muscle_mass: 'Khối lượng cơ (kg)', note: 'Ghi chú',
  duration_days: 'Số ngày hiệu lực của gói', session_count: 'Số buổi của gói trong danh mục',
  price: 'Giá bán (VND)', package_type: 'Loại gói đã mua', start_date: 'Ngày bắt đầu', end_date: 'Ngày kết thúc',
  total_sessions: 'Tổng số buổi của gói đã mua', remaining_sessions: 'Số buổi còn lại',
  freeze_count: 'Số lần bảo lưu', version: 'Phiên bản dùng kiểm soát cập nhật đồng thời',
  from_date: 'Ngày bắt đầu bảo lưu', to_date: 'Ngày kết thúc bảo lưu', reason: 'Lý do yêu cầu',
  decided_at: 'Thời điểm ra quyết định', fee: 'Phí chuyển nhượng (VND)', method: 'Phương thức thực hiện',
  match_score: 'Kết quả so khớp khuôn mặt', checkin_date: 'Ngày check-in theo giờ Việt Nam',
  session_deducted: 'Lượt check-in có trừ buổi hay không', checked_in_at: 'Thời điểm vào phòng tập',
  checked_out_at: 'Thời điểm ra khỏi phòng tập', day_of_week: 'Thứ trong tuần theo ISO',
  start_time: 'Giờ bắt đầu', end_time: 'Giờ kết thúc', valid_from: 'Ngày bắt đầu áp dụng lịch',
  valid_to: 'Ngày kết thúc áp dụng lịch', occurrence_date: 'Ngày của buổi theo lịch lặp khi sinh',
  start_at: 'Thời điểm bắt đầu', end_at: 'Thời điểm kết thúc', original_start_at: 'Thời điểm bắt đầu trước khi đổi giờ',
  waitlisted_at: 'Thời điểm vào danh sách chờ', booked_at: 'Thời điểm có chỗ đặt',
  promoted_at: 'Thời điểm được đưa từ danh sách chờ vào lớp', grace_until: 'Thời điểm kết thúc ân hạn hủy',
  reconfirm_expires_at: 'Hạn xác nhận lại sau đổi giờ', arrived_at: 'Thời điểm ghi nhận đã đến tại quầy',
  cancelled_at: 'Thời điểm hủy', cancel_reason: 'Lý do hủy', is_late_cancel: 'Có bị tính hủy muộn hay không',
  confirmed_at: 'Thời điểm xác nhận buổi PT đã diễn ra', commission_amount: 'Tiền hoa hồng PT (VND)',
  logged_at: 'Ngày ghi nhật ký tập', exercise: 'Tên bài tập', sets: 'Số hiệp', reps: 'Số lần lặp',
  sku: 'Mã hàng hóa', unit: 'Đơn vị tính', quantity: 'Số lượng', min_quantity: 'Mức tồn kho cảnh báo',
  unit_cost: 'Đơn giá nhập (VND)', value: 'Giá trị khuyến mãi theo loại', max_discount: 'Mức giảm tối đa (VND)',
  min_amount: 'Giá trị hóa đơn tối thiểu (VND)', usage_limit: 'Giới hạn lượt sử dụng', used_count: 'Số lượt đã sử dụng',
  source: 'Nguồn tạo hóa đơn', subtotal: 'Tổng tiền trước giảm giá (VND)', discount: 'Số tiền giảm (VND)',
  total: 'Tổng tiền (VND)', paid_at: 'Thời điểm thanh toán', refunded_at: 'Thời điểm hoàn tiền',
  refund_reason: 'Lý do hoàn tiền', item_type: 'Loại dòng hóa đơn', unit_price: 'Đơn giá lúc bán (VND)',
  amount: 'Số tiền (VND)', discount_amount: 'Số tiền giảm phân bổ cho dòng (VND)',
  transaction_ref: 'Thông tin tham chiếu chuyển khoản', staff_code: 'Mã nhân viên',
  base_salary: 'Lương cơ bản (VND)', hourly_rate: 'Mức lương theo giờ (VND)',
  commission_rate: 'Tỷ lệ hoa hồng PT', specialties: 'Chuyên môn huấn luyện', hired_at: 'Ngày tuyển dụng',
  check_in_at: 'Thời điểm vào ca thực tế', check_out_at: 'Thời điểm ra ca thực tế',
  month: 'Tháng tính lương', base_amount: 'Tiền lương cơ bản trong kỳ (VND)', deduction: 'Khoản khấu trừ lương (VND)',
  icon_url: 'Đường dẫn biểu tượng huy hiệu', rule: 'Quy tắc đạt huy hiệu', earned_at: 'Thời điểm đạt huy hiệu',
};

const lines = [
  '# Từ điển dữ liệu – ERD v2', '',
  '- **Người biên soạn:** Nguyễn Anh Tú (`natus26`) · [#15](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/15) · WBS 1.3.11.',
  '- **Cập nhật:** 05/11/2026.',
  '- **Nguồn chuẩn:** [erd-v2.dbml](erd-v2.dbml); mục đích bảng theo [thiết kế dữ liệu](thiet-ke-du-lieu.md).',
  `- **Phạm vi:** ${tables.length} bảng, ${tables.reduce((n,t)=>n+t.fields.length,0)} cột, ${schema.refs.length} quan hệ, ${tables.reduce((n,t)=>n+t.indexes.length,0)} khai báo chỉ mục và ${enums.size} tập giá trị Enum.`,
  `- **SHA-256 DBML (chuẩn hóa xuống dòng LF):** ${code(checksum)}.`, '',
  '## 1. Cách đọc và cập nhật', '',
  'Đây là bản xuất phục vụ tra cứu và báo cáo; khi khác nhau thì DBML là đúng. Chạy lại công cụ theo [hướng dẫn xuất bản](xuat-ban-erd.md) sau khi nguồn chuẩn thay đổi, không sửa tay các bảng dưới đây.', '',
  '- Cột **Kiểu DBML** giữ nguyên khai báo nguồn. Enum dùng để vẽ; migration lưu `varchar` kèm `CHECK`, không phải kiểu Enum PostgreSQL. Độ dài `varchar` thực tế của các cột Enum xem migration đã duyệt.',
  '- **Rỗng** cho biết được phép NULL hay không; khóa chính luôn không rỗng. `PK` là khóa chính, `FK` là khóa ngoại, `UNIQUE` là duy nhất, `DEFAULT` là mặc định, `IDENTITY` diễn giải thuộc tính `increment` của DBML.',
  '- Ràng buộc trong ngoặc vuông, chỉ mục và ghi chú được giữ riêng: `CHECK`, chỉ mục có điều kiện và `NULLS NOT DISTINCT` mô tả trong ghi chú phải được triển khai bằng migration hoặc mã nghiệp vụ như nguồn yêu cầu; không coi DBML đã tự thực thi chúng.',
  '- Cột **Ý nghĩa / ghi chú** có diễn giải ngắn theo tên trường, sau đó giữ ghi chú DBML nếu có. Với FK, đích tham chiếu được lấy từ khai báo nguồn. Không thêm quy tắc mới từ việc diễn giải tên.',
  '- Thời điểm `timestamptz` lưu UTC; ngày theo nghiệp vụ Việt Nam được ghi trong ghi chú tương ứng. Tiền là số nguyên VND; số đo cơ thể, khối lượng và tỷ lệ hoa hồng giữ kiểu thập phân trong DBML.', '',
  '## 2. Chỉ mục bảng theo nhóm', '',
  '| Nhóm | Bảng | Số cột | Mục đích |', '| --- | --- | ---: | --- |',
];
for (const group of groups) for (const name of group.tables) {
  const table = tables.find(t => t.name === name);
  if (!table || !purposes.has(name)) throw new Error('Thiếu bảng hoặc ý nghĩa bảng: '+name);
  lines.push(`| ${group.number}. ${escape(group.title)} | [${code(name)}](#bang-${name}) | ${table.fields.length} | ${escape(purposes.get(name))} |`);
}
lines.push('', '## 3. Chi tiết cột, ràng buộc và chỉ mục', '');
for (const group of groups) {
  lines.push(`### Nhóm ${group.number}. ${group.title}`, '');
  for (const name of group.tables) {
    const table = tables.find(t => t.name === name);
    lines.push(`<a id="bang-${name}"></a>`, '', `#### ${code(name)}`, '',
      `${escape(purposes.get(name))}. [Khai báo nguồn](erd-v2.dbml#L${locations.get(name)}).`, '',
      '| Cột | Kiểu DBML | Rỗng | Ràng buộc khai báo | Ý nghĩa / ghi chú |', '| --- | --- | --- | --- | --- |');
    for (const field of table.fields) {
      const fk = foreignKeys.get(`${name}.${field.name}`);
      const constraints = [];
      if (field.pk) constraints.push('PK');
      if (field.increment) constraints.push('IDENTITY');
      if (field.unique) constraints.push('UNIQUE');
      if (fk) constraints.push(`FK → ${code(fk.table+'.'+fk.field)}${fk.relation==='-'?' (1–1)':''}`);
      if (field.dbdefault) {
        const d = field.dbdefault;
        constraints.push('DEFAULT '+code(d.type==='string'?`'${d.value}'`:String(d.value)));
      }
      const en = enums.get(field.type.type_name);
      if (en) constraints.push(`[Tập giá trị](#enum-${en.name})`);
      const meaning = labels[field.name] || (fk ? `Tham chiếu ${fk.table}.${fk.field}` : null);
      if (!meaning) throw new Error('Thiếu diễn giải trường: '+name+'.'+field.name);
      const description = escape(meaning)+(field.note?'<br>DBML: '+escape(field.note):'');
      lines.push(`| ${code(field.name)} | ${code(field.type.type_name)} | ${field.pk||field.not_null?'Không':'Có'} | ${constraints.join('; ')||'—'} | ${description} |`);
    }
    if (table.indexes.length) {
      lines.push('', '**Chỉ mục khai báo thêm** (khóa chính và UNIQUE trên cột đã nằm trong bảng cột):', '',
        '| Tên trong DBML | Cột | Duy nhất | Ghi chú / điều kiện |', '| --- | --- | --- | --- |');
      for (const idx of table.indexes) {
        lines.push(`| ${idx.name?code(idx.name):'Không đặt tên'} | ${idx.columns.map(c=>code(c.value)).join(', ')} | ${idx.unique?'Có':'Không'} | ${idx.note?escape(idx.note):'—'} |`);
      }
    }
    if (table.note) lines.push('', '**Ghi chú cấp bảng từ DBML:** '+escape(table.note));
    lines.push('');
  }
}
lines.push('## 4. Tập giá trị Enum trong DBML', '',
  'Các tập giá trị sau được giữ theo nguồn; việc chuyển sang `varchar` và `CHECK` thực hiện trong migration.', '');
for (const en of enums.values()) {
  lines.push(`<a id="enum-${en.name}"></a>`, '', `### ${code(en.name)}`, '',
    '| Giá trị | Ghi chú trong nguồn |', '| --- | --- |');
  for (const v of en.values) lines.push(`| ${code(v.name)} | ${v.note?escape(v.note):'—'} |`);
  lines.push('');
}
lines.push('## 5. Lịch sử thay đổi', '', '| Ngày | Nội dung |', '| --- | --- |',
  '| 05/11/2026 | Xuất từ điển đầy đủ từ ERD v2 cho #15; giữ cột, kiểu, mặc định, khóa, chỉ mục, Enum và ghi chú của nguồn |', '');
const output = lines.join('\n');
if (args.includes('--check')) {
  const existing = fs.existsSync(outputPath) ? fs.readFileSync(outputPath, 'utf8').replace(/\r\n/g,'\n') : '';
  if (existing !== output) {
    console.error('Từ điển khác nguồn hoặc công cụ sinh; chạy lại không có --check.');
    process.exit(1);
  }
  console.log(`Khớp nguồn: ${tables.length} bảng, ${tables.reduce((n,t)=>n+t.fields.length,0)} cột, ${foreignKeys.size} FK.`);
} else {
  fs.writeFileSync(outputPath, output, 'utf8');
  console.log('Đã sinh '+path.relative(root, outputPath));
}
