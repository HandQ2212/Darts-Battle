#!/usr/bin/env python3
"""Build a personal report .docx from the group report + a body DSL file.

Usage: python3 build_report.py <person> <body.txt> <out.docx>
  person: linh | hai | luc | hieu

Front matter (cover, TOC, abbreviations, lists of figures/tables, phân công page)
is copied verbatim from the original; the person's name + MSSV are bolded.
Everything from "LỜI MỞ ĐẦU" onward is produced from body.txt.

Body DSL (one paragraph per line, blank lines ignored, lines starting with // are comments):
  @ORIG 47-63         copy original body elements 47..63 verbatim (see orig_index.txt)
  @ORIG 88            copy one original element
  # Title             chapter heading (BTL-H1, auto-numbered "CHƯƠNG n")
  ## Title            BTL-H2 (n.m)
  ### Title           BTL-H3 (n.m.k)
  @H0 Title           unnumbered heading (LỜI MỞ ĐẦU, KẾT LUẬN, TÀI LIỆU THAM KHẢO)
  - text              bullet
  @REF text           numbered reference entry [n]
  @IMG path | width_cm | caption      image (png/jpeg) + auto-numbered "Hình" caption
  @TABLE caption | w1,w2,...          caption (auto "Bảng"), then following lines
  | a | b | c |                       are rows (first row = header) until a non-| line.
                                      widths are relative weights.
  @CODE text          monospace line (Courier New 11)
  @PAGEBREAK
  any other line      normal paragraph (BTL-Text, first-line indent, justified)
Inline: **bold**, *italic*.
"""
import os, re, sys, shutil, subprocess, zipfile, html

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from docxsplit import split_body

ORIG = os.path.join(HERE, 'orig.docx')
PEOPLE = {
    'linh': ('Văn Thị Mai Linh', 'B23DCCN489'),
    'hai': ('Nguyễn Nam Hải', 'B23DCCN277'),
    'luc': ('Nguyễn Văn Minh Lực', 'B23DCCN516'),
    'hieu': ('Giáp Minh Hiếu', 'B23DCCN299'),
}
FRONT_END = 37      # elements 0..37 kept (38 = "[Ghi chú...]" dropped)
LANG = '<w:lang w:val="vi-VN"/>'


def esc(s):
    return html.escape(s, quote=False)


def runs(text, extra_rpr=''):
    out = []
    for part in re.split(r'(\*\*[^*]+\*\*|\*[^*]+\*)', text):
        if not part:
            continue
        rpr = extra_rpr
        if part.startswith('**') and part.endswith('**'):
            part = part[2:-2]; rpr += '<w:b/><w:bCs/>'
        elif part.startswith('*') and part.endswith('*') and len(part) > 2:
            part = part[1:-1]; rpr += '<w:i/><w:iCs/>'
        out.append('<w:r><w:rPr>%s%s</w:rPr><w:t xml:space="preserve">%s</w:t></w:r>' % (rpr, LANG, esc(part)))
    return ''.join(out)


def para(style, text, ppr_extra='', rpr=''):
    st = '<w:pStyle w:val="%s"/>' % style if style else ''
    return '<w:p><w:pPr>%s%s</w:pPr>%s</w:p>' % (st, ppr_extra, runs(text, rpr))


BULLET_PPR = '<w:tabs><w:tab w:val="left" w:pos="720"/></w:tabs><w:ind w:left="720" w:hanging="360"/>'


class Builder:
    def __init__(self, workdir):
        self.wd = workdir
        self.rels_path = os.path.join(workdir, 'word', '_rels', 'document.xml.rels')
        self.rels = open(self.rels_path, encoding='utf8').read()
        self.next_rid = 1000
        self.next_doc = 5000
        self.img_n = 0

    def add_image(self, path, width_cm):
        path = os.path.abspath(path)
        ext = os.path.splitext(path)[1].lower().lstrip('.')
        if ext == 'jpg':
            ext = 'jpeg'
        if ext not in ('png', 'jpeg'):
            sys.exit('Unsupported image type: ' + path)
        self.img_n += 1
        name = 'pimg%d.%s' % (self.img_n, ext)
        shutil.copy(path, os.path.join(self.wd, 'word', 'media', name))
        rid = 'rId%d' % self.next_rid; self.next_rid += 1
        self.rels = self.rels.replace('</Relationships>',
            '<Relationship Id="%s" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" Target="media/%s"/></Relationships>' % (rid, name))
        out = subprocess.run(['sips', '-g', 'pixelWidth', '-g', 'pixelHeight', path], capture_output=True, text=True).stdout
        w = int(re.search(r'pixelWidth: (\d+)', out).group(1)); h = int(re.search(r'pixelHeight: (\d+)', out).group(1))
        width_cm = min(float(width_cm), 16.5)
        cx = int(width_cm * 360000); cy = int(cx * h / w)
        max_cy = int(22 * 360000)
        if cy > max_cy:
            cy = max_cy; cx = int(cy * w / h)
        did = self.next_doc; self.next_doc += 1
        drawing = ('<w:r><w:drawing><wp:inline distT="0" distB="0" distL="0" distR="0"><wp:extent cx="{cx}" cy="{cy}"/>'
                   '<wp:effectExtent l="0" t="0" r="0" b="0"/><wp:docPr id="{d}" name="Hinh {d}"/><wp:cNvGraphicFramePr/>'
                   '<a:graphic xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"><a:graphicData uri="http://schemas.openxmlformats.org/drawingml/2006/picture">'
                   '<pic:pic xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture"><pic:nvPicPr><pic:cNvPr id="{d}" name="Hinh {d}"/><pic:cNvPicPr/></pic:nvPicPr>'
                   '<pic:blipFill><a:blip r:embed="{rid}"/><a:stretch><a:fillRect/></a:stretch></pic:blipFill><pic:spPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="{cx}" cy="{cy}"/></a:xfrm>'
                   '<a:prstGeom prst="rect"><a:avLst/></a:prstGeom></pic:spPr></pic:pic></a:graphicData></a:graphic></wp:inline></w:drawing></w:r>').format(cx=cx, cy=cy, d=did, rid=rid)
        return ('<w:p><w:pPr><w:keepNext/><w:spacing w:before="120" w:after="0" w:line="240" w:lineRule="auto"/><w:jc w:val="center"/></w:pPr>%s</w:p>' % drawing)

    def save(self):
        open(self.rels_path, 'w', encoding='utf8').write(self.rels)


def table_xml(rows, weights):
    ncol = max(len(r) for r in rows)
    weights = (weights + [1] * ncol)[:ncol]
    total = 9400
    ws = [int(total * w / sum(weights)) for w in weights]
    grid = ''.join('<w:gridCol w:w="%d"/>' % w for w in ws)
    tblpr = ('<w:tblPr><w:tblStyle w:val="23"/><w:tblW w:w="%d" w:type="dxa"/><w:jc w:val="center"/><w:tblBorders>'
             '<w:top w:val="single" w:color="auto" w:sz="4" w:space="0"/><w:left w:val="single" w:color="auto" w:sz="4" w:space="0"/>'
             '<w:bottom w:val="single" w:color="auto" w:sz="4" w:space="0"/><w:right w:val="single" w:color="auto" w:sz="4" w:space="0"/>'
             '<w:insideH w:val="single" w:color="auto" w:sz="4" w:space="0"/><w:insideV w:val="single" w:color="auto" w:sz="4" w:space="0"/>'
             '</w:tblBorders><w:tblLayout w:type="fixed"/><w:tblCellMar><w:top w:w="0" w:type="dxa"/><w:left w:w="85" w:type="dxa"/>'
             '<w:bottom w:w="0" w:type="dxa"/><w:right w:w="85" w:type="dxa"/></w:tblCellMar></w:tblPr>') % sum(ws)
    out = ['<w:tbl>', tblpr, '<w:tblGrid>%s</w:tblGrid>' % grid]
    for ri, r in enumerate(rows):
        r = r + [''] * (ncol - len(r))
        hdr = ri == 0
        out.append('<w:tr><w:trPr><w:cantSplit/>%s<w:jc w:val="center"/></w:trPr>' % ('<w:tblHeader/>' if hdr else ''))
        for ci, cell in enumerate(r):
            shd = '<w:shd w:val="clear" w:color="auto" w:fill="D9E2F3"/>' if hdr else ''
            jc = 'center' if hdr else 'left'
            rpr = '<w:sz w:val="24"/><w:szCs w:val="24"/>' + ('<w:b/><w:bCs/>' if hdr else '')
            paras = ''.join('<w:p><w:pPr><w:spacing w:before="40" w:after="40" w:line="240" w:lineRule="auto"/><w:jc w:val="%s"/></w:pPr>%s</w:p>'
                            % (jc, runs(seg.strip(), rpr)) for seg in cell.split('<br>'))
            out.append('<w:tc><w:tcPr><w:tcW w:w="%d" w:type="dxa"/>%s<w:vAlign w:val="center"/></w:tcPr>%s</w:tc>' % (ws[ci], shd, paras))
        out.append('</w:tr>')
    out.append('</w:tbl>')
    out.append('<w:p><w:pPr><w:spacing w:after="0"/></w:pPr></w:p>')
    return ''.join(out)


def bold_names(el, name, mssv):
    def fix(m):
        r = m.group(0)
        if name not in r and mssv not in r:
            return r
        r = r.replace('<w:b w:val="0"/>', '').replace('<w:bCs w:val="0"/>', '')
        if '<w:rPr>' in r:
            return r.replace('<w:rPr>', '<w:rPr><w:b/><w:bCs/>', 1)
        return r.replace('<w:r>', '<w:r><w:rPr><w:b/><w:bCs/></w:rPr>', 1)
    return re.sub(r'<w:r>(?:(?!</w:r>).)*?</w:r>', fix, el, flags=re.S)


def build(person, body_path, out_path):
    name, mssv = PEOPLE[person]
    wd = out_path + '.work'
    shutil.rmtree(wd, ignore_errors=True)
    with zipfile.ZipFile(ORIG) as z:
        z.extractall(wd)
    docp = os.path.join(wd, 'word', 'document.xml')
    head, els, sect, tail = split_body(open(docp, encoding='utf8').read())
    b = Builder(wd)
    front = [bold_names(e, name, mssv) if i in (15, 36) else e for i, e in enumerate(els[:FRONT_END + 1])]
    body = []
    lines = open(body_path, encoding='utf8').read().splitlines()
    i = 0
    while i < len(lines):
        ln = lines[i].rstrip(); i += 1
        s = ln.strip()
        if not s or s.startswith('//'):
            continue
        if s.startswith('@ORIG'):
            spec = s[5:].strip()
            for part in spec.split(','):
                part = part.strip()
                if '-' in part:
                    a, c = map(int, part.split('-'))
                else:
                    a = c = int(part)
                if a <= FRONT_END:
                    sys.exit('@ORIG may only reference elements > %d' % FRONT_END)
                body.extend(els[a:c + 1])
        elif s.startswith('#### '):
            body.append(para('45', s[5:]))
        elif s.startswith('### '):
            body.append(para('43', s[4:]))
        elif s.startswith('## '):
            body.append(para('41', s[3:]))
        elif s.startswith('# '):
            body.append(para('40', s[2:]))
        elif s.startswith('@H0 '):
            body.append(para('47', s[4:]))
        elif s.startswith('- '):
            body.append(para('30', s[2:], BULLET_PPR))
        elif s.startswith('@REF '):
            body.append(para('70', s[5:]))
        elif s.startswith('@CODE '):
            body.append(para('', s[6:], '<w:spacing w:after="0" w:line="240" w:lineRule="auto"/><w:ind w:left="454"/><w:jc w:val="left"/>',
                             '<w:rFonts w:ascii="Courier New" w:hAnsi="Courier New" w:cs="Courier New"/><w:sz w:val="21"/><w:szCs w:val="21"/>'))
        elif s == '@PAGEBREAK':
            body.append('<w:p><w:r><w:br w:type="page"/></w:r></w:p>')
        elif s.startswith('@IMG '):
            parts = [p.strip() for p in s[5:].split('|')]
            path = parts[0]
            if not os.path.isabs(path):
                path = os.path.join(os.path.dirname(os.path.abspath(body_path)), path)
            body.append(b.add_image(path, parts[1] if len(parts) > 1 and parts[1] else 15))
            if len(parts) > 2 and parts[2]:
                body.append(para('57', parts[2]))
        elif s.startswith('@TABLE '):
            parts = [p.strip() for p in s[7:].split('|')]
            weights = [float(w) for w in parts[1].split(',')] if len(parts) > 1 and parts[1] else []
            rows = []
            while i < len(lines) and lines[i].strip().startswith('|'):
                r = lines[i].strip(); i += 1
                cells = [c.strip() for c in r.strip('|').split('|')]
                if all(re.fullmatch(r':?-{2,}:?', c) for c in cells if c):
                    continue
                rows.append(cells)
            if parts[0]:
                body.append(para('51', parts[0]))
            body.append(table_xml(rows, weights))
        else:
            body.append(para('32', s))
    # keep headings and table captions with the next paragraph (no orphaned titles at page bottom)
    def keep_next(e):
        if e.startswith('<w:p') and re.search(r'<w:pStyle w:val="(40|41|43|45|47|51)"/>', e) and '<w:keepNext/>' not in e:
            return re.sub(r'(<w:pStyle w:val="[^"]+"/>)', r'\1<w:keepNext/>', e, count=1)
        return e
    body = [keep_next(e) for e in body]
    b.save()
    open(docp, 'w', encoding='utf8').write(head + ''.join(front + body) + sect + tail)
    if os.path.exists(out_path):
        os.remove(out_path)
    with zipfile.ZipFile(out_path, 'w', zipfile.ZIP_DEFLATED) as z:
        for root, _, files in os.walk(wd):
            for f in files:
                full = os.path.join(root, f)
                arc = os.path.relpath(full, wd)
                z.write(full, arc)
    shutil.rmtree(wd)
    # sanity: XML must parse
    import xml.dom.minidom
    with zipfile.ZipFile(out_path) as z:
        xml.dom.minidom.parseString(z.read('word/document.xml'))
    print('OK ->', out_path)


if __name__ == '__main__':
    if len(sys.argv) != 4:
        sys.exit(__doc__)
    build(*sys.argv[1:])
