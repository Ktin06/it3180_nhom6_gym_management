#!/usr/bin/env node
// Bản dựng ERD cục bộ từ DBML; không phải bản xuất từ dbdiagram.io.
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const root = path.resolve(__dirname, '..');
const args = process.argv.slice(2);
function arg(name, required = true) {
  const i = args.indexOf(name);
  if (i >= 0 && args[i + 1]) return args[i + 1];
  if (required) throw new Error('Thiếu tham số '+name);
}
const html = value => String(value).replace(/&/g,'&amp;').replace(/</g,'&lt;')
  .replace(/>/g,'&gt;').replace(/"/g,'&quot;');
const quote = value => JSON.stringify(String(value));

async function main() {
  const corePath = path.resolve(arg('--dbml-core'));
  const vizPath = path.resolve(arg('--viz'));
  const { Parser, ModelExporter } = require(corePath);
  const { instance } = require(vizPath);
  const source = fs.readFileSync(path.join(root,'docs/design/erd/erd-v2.dbml'),'utf8').replace(/\r\n/g,'\n');
  const schema = JSON.parse(ModelExporter.export(new Parser().parse(source,'dbmlv2'),'json',{isNormalized:false})).schemas[0];
  const groups = [];
  const groupByTable = new Map();
  const refs = [];
  let group, tableName;
  for (const line of source.split('\n')) {
    const g=line.match(/^\/\/ (\d+)\. (.+?)(?: \(\d+ bảng\))?(?: –.*)?$/u);
    if (g) { group={id:Number(g[1]),title:g[2],names:[]}; groups.push(group); }
    const t=line.match(/^Table (\w+) \{/);
    if (t) { tableName=t[1]; group.names.push(tableName); groupByTable.set(tableName,group.id); }
    const r=line.match(/^\s+(\w+)\s+.+?\[.*?ref:\s*([><-])\s*(\w+)\.(\w+)/);
    if (r) refs.push({child:tableName,field:r[1],relation:r[2],parent:r[3],parentField:r[4]});
  }
  if (refs.length!==schema.refs.length) throw new Error('Không đủ quan hệ trong hình.');
  const tables=new Map(schema.tables.map(t=>[t.name,t]));
  const foreignFields=new Set(refs.map(r=>r.child+'.'+r.field));
  const colors=['#dbeafe','#dcfce7','#fef3c7','#e0e7ff','#fce7f3','#ffedd5','#cffafe','#ede9fe'];
  const viz=await instance();
  const output=path.join(root,'docs/design/erd/hinh');
  fs.mkdirSync(output,{recursive:true});
  const manifest={source:'../erd-v2.dbml',sha256LF:crypto.createHash('sha256').update(source).digest('hex'),
    generator:'scripts/export-erd.cjs',renderer:'Graphviz qua @viz-js/viz (cục bộ)',graphvizVersion:viz.graphvizVersion,
    tables:tables.size,columns:schema.tables.reduce((n,t)=>n+t.fields.length,0),relationships:refs.length,figures:[]};

  function node(name, compact=false) {
    const t=tables.get(name);
    const color=compact?'#f1f5f9':colors[groupByTable.get(name)-1];
    const fields=compact?t.fields.filter(f=>f.pk):t.fields;
    const rows=fields.map(f=>{
      const key=[f.pk?'PK':'',foreignFields.has(name+'.'+f.name)?'FK':''].filter(Boolean).join('/');
      const type=f.type.type_name+(f.pk||f.not_null?'':' ?');
      return `<TR><TD PORT="${html(f.name)}_left" ALIGN="LEFT">${key?'<B>'+key+'</B> ':''}${html(f.name)}</TD><TD PORT="${html(f.name)}_right" ALIGN="LEFT"><FONT COLOR="#475569">${html(type)}</FONT></TD></TR>`;
    }).join('');
    return `${quote(name)} [id=${quote('table-'+name)},label=<<TABLE BORDER="1" CELLBORDER="0" CELLSPACING="0" CELLPADDING="5" COLOR="#64748b" BGCOLOR="white"><TR><TD COLSPAN="2" BGCOLOR="${color}"><FONT POINT-SIZE="14"><B>${html(name)}</B></FONT>${compact?'<BR/><FONT POINT-SIZE="10">tham chiếu ngoài nhóm</FONT>':''}</TD></TR>${rows}</TABLE>>];`;
  }

  function build(selected) {
    const names=selected?selected.names:[...tables.keys()];
    const set=new Set(names);
    const edges=refs.filter(r=>set.has(r.child));
    const external=[...new Set(edges.filter(r=>!set.has(r.parent)).map(r=>r.parent))];
    const title=selected?`Nhóm ${selected.id}. ${selected.title}`:`ERD v2 — ${tables.size} bảng, ${refs.length} quan hệ`;
    const dot=[
      'digraph erd {',
      `graph [rankdir=${selected?'LR':'TB'}, compound=true, newrank=true, bgcolor="white", pad="0.4", nodesep="0.45", ranksep="1.05", splines=polyline, fontname="Arial", fontsize=21, labelloc=t, label=${quote(title+'\nPK: khóa chính · FK: khóa ngoại · ?: được NULL\nĐường nối từ bảng cha tới bảng con; dấu chân quạ: nhiều · vạch: một · tròn: tùy chọn')}];`,
      'node [shape=plain,fontname="Arial",fontsize=11];',
      'edge [color="#64748b",penwidth=1.05,arrowsize=0.65,dir=both];',
    ];
    for (const g of (selected?[selected]:groups)) {
      dot.push(`subgraph cluster_${g.id} { label=${quote(g.id+'. '+g.title)}; fontname="Arial"; fontsize=17; color="${colors[g.id-1]}"; style="rounded"; penwidth=3;`);
      for (const name of g.names) dot.push(node(name));
      dot.push('}');
    }
    for(const name of external) dot.push(node(name,true));
    for(const r of edges) {
      const field=tables.get(r.child).fields.find(f=>f.name===r.field);
      const one=r.relation==='-'||field.unique;
      const tail=field.pk||field.not_null?'teetee':'odottee';
      const head=one?'odottee':'crowodot';
      dot.push(`${quote(r.parent)}:${quote(r.parentField+'_right')}:e -> ${quote(r.child)}:${quote(r.field+'_left')}:w [id=${quote('ref-'+r.child+'-'+r.field)},arrowtail=${quote(tail)},arrowhead=${quote(head)}];`);
    }
    dot.push('}');
    return {dot:dot.join('\n'),names,external,edges};
  }

  for(const selected of [null,...groups]) {
    const model=build(selected);
    const name=selected?'nhom-'+selected.id:'erd-v2';
    const rendered=viz.render(model.dot,{engine:'dot',format:'svg'});
    if(rendered.status!=='success'||rendered.errors.some(e=>e.level==='error')) throw new Error(JSON.stringify(rendered.errors));
    if(rendered.errors.length) console.warn(name+': '+rendered.errors.map(e=>e.message).join('; '));
    const svg=rendered.output;
    fs.writeFileSync(path.join(output,name+'.svg'),svg,'utf8');
    const view=svg.match(/viewBox="([^"]+)"/)[1].split(/\s+/).map(Number);
    const figure={file:name+'.svg',tables:model.names,externalTables:model.external,relationships:model.edges.length,widthPt:view[2],heightPt:view[3]};
    manifest.figures.push(figure);
  }
  const playwrightPath=arg('--playwright',false);
  if(playwrightPath) {
    const {chromium}=require(path.resolve(playwrightPath));
    const browser=await chromium.launch({headless:true,executablePath:path.resolve(arg('--chromium')),args:['--disable-extensions']});
    try {
      const page=await browser.newPage();
      for(const figure of manifest.figures) {
        const svg=fs.readFileSync(path.join(output,figure.file),'utf8').replace(/<\?xml[^>]*\?>/,'').replace(/<!DOCTYPE[\s\S]*?>/,'');
        const width=figure.widthPt, height=figure.heightPt;
        await page.setViewportSize({width:Math.ceil(width*96/72),height:Math.min(1000,Math.ceil(height*96/72))});
        await page.setContent(`<!doctype html><html><head><meta charset="utf-8"><style>@page {size:${width}pt ${height}pt;margin:0}html,body{margin:0;width:${width}pt;height:${height}pt}svg{display:block;width:100%;height:100%}</style></head><body>${svg}</body></html>`);
        await page.evaluate(()=>document.fonts.ready);
        const base=figure.file.replace(/\.svg$/,'');
        await page.pdf({path:path.join(output,base+'.pdf'),preferCSSPageSize:true,printBackground:true});
        if(base==='erd-v2') await page.screenshot({path:path.join(output,base+'.png'),fullPage:true});
        figure.pdf=base+'.pdf';
      }
    } finally { await browser.close(); }
  }
  fs.writeFileSync(path.join(output,'nguon-hinh.json'),JSON.stringify(manifest,null,2)+'\n','utf8');
  console.log(`Đã xuất ${manifest.figures.length} hình SVG${playwrightPath?', 9 PDF và PNG tổng quan':''}.`);
}
main().catch(error=>{console.error(error.message);process.exitCode=1;});
