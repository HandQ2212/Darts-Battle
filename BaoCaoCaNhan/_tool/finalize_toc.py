#!/usr/bin/env python3
"""Page numbering + generated TOC / list of figures / list of tables for a built report.

Usage: python3 finalize_toc.py <report.docx>   (modifies the file in place)

- Cover: no page number. Front matter (Mục lục … Phân công): i, ii, iii…
  Body (from LỜI MỞ ĐẦU): 1, 2, 3…  (section break inserted before LỜI MỞ ĐẦU)
- Bookmarks are added to every heading and caption; the three TOC fields are
  filled with real entries (hyperlinked) whose page numbers come from a
  LibreOffice render of the document. Fields stay live: F9 in Word/WPS refreshes them.
Requires LibreOffice (soffice) and pdftotext.
"""
import html, os, re, shutil, subprocess, sys, tempfile, zipfile

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from docxsplit import split_body

SOFFICE = shutil.which('soffice') or '/Applications/LibreOffice.app/Contents/MacOS/soffice'
HEAD_LVL = {'47': 1, '40': 1, '41': 2, '43': 3}          # style -> toc level
TOC_STYLE = {1: '26', 2: '27', 3: '28'}
RIGHT_TAB = '<w:tabs><w:tab w:val="right" w:leader="dot" w:pos="9458"/></w:tabs>'


def text_of(e):
    return html.unescape(''.join(re.findall(r'<w:t(?:\s[^>]*)?>([^<]*)</w:t>', e)))


def style_of(e):
    m = re.search(r'<w:pStyle w:val="([^"]+)"', e)
    return m.group(1) if m else ''


def esc(s):
    return html.escape(s, quote=False)


def norm(s):
    return re.sub(r'\s+', '', s).lower()


def collect(els, body_start):
    """Return entries in document order: dict(idx, kind, level, label, text)."""
    entries = []
    h = [0, 0, 0]
    fig = tab = 0
    for i, e in enumerate(els):
        if not e.startswith('<w:p'):
            continue
        st = style_of(e); t = text_of(e).strip()
        if not t:
            continue
        if st in HEAD_LVL:
            lvl = HEAD_LVL[st]; label = ''
            if st == '40':
                h = [h[0] + 1, 0, 0]; label = '%d.' % h[0]
            elif st == '41':
                h = [h[0], h[1] + 1, 0]; label = '%d.%d' % (h[0], h[1])
            elif st == '43':
                h[2] += 1; label = '%d.%d.%d' % tuple(h)
            entries.append(dict(idx=i, kind='toc', level=lvl, label=label, text=t))
        elif st == '57':
            fig += 1
            entries.append(dict(idx=i, kind='fig', level=1, label='Hình %d' % fig, text=t))
        elif st == '51':
            tab += 1
            entries.append(dict(idx=i, kind='tab', level=1, label='Bảng %d.' % tab, text=t))
    return entries


def add_bookmarks(els, entries):
    for n, en in enumerate(entries):
        en['bm'] = '_Toc%09d' % (500000000 + n)
        e = els[en['idx']]
        bid = 90000 + n
        m = re.search(r'</w:pPr>', e)
        pos = m.end() if m else re.match(r'<w:p[^>]*>', e).end()
        els[en['idx']] = (e[:pos] + '<w:bookmarkStart w:id="%d" w:name="%s"/>' % (bid, en['bm'])
                          + e[pos:-len('</w:p>')] + '<w:bookmarkEnd w:id="%d"/></w:p>' % bid)


def toc_block(instr, items, empty_text):
    """items: list of (style, bold, anchor, label_text, page)."""
    begin = ('<w:r><w:fldChar w:fldCharType="begin"/></w:r><w:r><w:instrText xml:space="preserve">%s</w:instrText></w:r>'
             '<w:r><w:fldChar w:fldCharType="separate"/></w:r>') % instr
    end = '<w:r><w:fldChar w:fldCharType="end"/></w:r>'
    if not items:
        return '<w:p><w:pPr><w:pStyle w:val="32"/><w:ind w:firstLine="0"/></w:pPr>%s<w:r><w:t>%s</w:t></w:r>%s</w:p>' % (begin, esc(empty_text), end)
    out = []
    for k, (st, bold, anchor, txt, page) in enumerate(items):
        rpr = '<w:rPr>%s<w:lang w:val="vi-VN"/></w:rPr>' % ('<w:b/><w:bCs/>' if bold else '')
        link = ('<w:hyperlink w:anchor="%s" w:history="1"><w:r>%s<w:t xml:space="preserve">%s</w:t></w:r>'
                '<w:r>%s<w:tab/></w:r><w:r>%s<w:t>%s</w:t></w:r></w:hyperlink>') % (anchor, rpr, esc(txt), rpr, rpr, page)
        out.append('<w:p><w:pPr><w:pStyle w:val="%s"/>%s<w:spacing w:after="60"/><w:jc w:val="left"/></w:pPr>%s%s%s</w:p>'
                   % (st, RIGHT_TAB, begin if k == 0 else '', link, end if k == len(items) - 1 else ''))
    return ''.join(out)


def build_lists(entries, pages):
    toc, figs, tabs = [], [], []
    for en in entries:
        p = pages.get(en['bm'], '')
        txt = (en['label'] + ' ' if en['label'] else '') + en['text']
        if en['kind'] == 'toc':
            lvl = en['level']
            toc.append((TOC_STYLE[lvl], lvl == 1, en['bm'], txt.upper() if lvl == 1 else txt, p))
        elif en['kind'] == 'fig':
            figs.append(('24', False, en['bm'], txt, p))
        else:
            tabs.append(('24', False, en['bm'], txt, p))
    return toc, figs, tabs


def find_field_paras(els):
    idx = {}
    for i, e in enumerate(els):
        m = re.search(r'<w:instrText[^>]*>([^<]*)</w:instrText>', e)
        if m and 'TOC' in m.group(1):
            ins = html.unescape(m.group(1))
            key = 'fig' if 'BTL-Hinh' in ins else 'tab' if 'BTL-Bang' in ins else 'toc'
            idx[key] = (i, m.group(1))
    return idx


def assemble(head, els, sect, tail, fields, lists, body_start):
    toc, figs, tabs = lists
    out = list(els)
    repl = {fields['toc'][0]: toc_block(fields['toc'][1], toc, 'Mục lục'),
            fields['fig'][0]: toc_block(fields['fig'][1], figs, 'Không có hình vẽ.'),
            fields['tab'][0]: toc_block(fields['tab'][1], tabs, 'Không có bảng biểu.')}
    for i, x in repl.items():
        out[i] = x
    # section break: front matter (roman), body (decimal from 1)
    front_sect = re.sub(r'<w:pgNumType[^>]*/>', '<w:pgNumType w:fmt="lowerRoman" w:start="1"/>', sect)
    body_sect = re.sub(r'<w:pgNumType[^>]*/>', '<w:pgNumType w:fmt="decimal" w:start="1"/>', sect)
    out.insert(body_start, '<w:p><w:pPr><w:spacing w:after="0" w:line="240" w:lineRule="auto"/>%s</w:pPr></w:p>' % front_sect)
    return head + ''.join(out) + body_sect + tail


def render_pages(docx, entries, body_start_idx):
    tmp = tempfile.mkdtemp()
    subprocess.run([SOFFICE, '--headless', '-env:UserInstallation=file://' + tmp + '/lo', '--convert-to', 'pdf',
                    '--outdir', tmp, docx], capture_output=True, timeout=300)
    pdf = os.path.join(tmp, os.path.splitext(os.path.basename(docx))[0] + '.pdf')
    if not os.path.exists(pdf):
        sys.exit('LibreOffice render failed')
    txt = subprocess.run(['pdftotext', '-layout', pdf, '-'], capture_output=True, text=True).stdout
    pages = [[norm(l) for l in p.splitlines() if l.strip()] for p in txt.split('\f')]
    shutil.copy(pdf, docx[:-5] + '.pdf')
    shutil.rmtree(tmp)

    def match(lines, target):
        for l in lines:
            if '....' in l or '…' in l or len(l) < 4:
                continue
            if l == target or (len(l) >= min(18, len(target)) and target.startswith(l)):
                return True
        return False

    found, cur = {}, 0
    toc_page = next(i for i, p in enumerate(pages) if 'mụclục' in p)
    body_page = None
    for en in entries:
        target = norm((en['label'] + ' ' if en['label'] else '') + en['text'])
        for p in range(cur, len(pages)):
            if match(pages[p], target):
                found[en['bm']] = p; cur = p; break
        else:
            print('WARN not found:', en['label'], en['text'][:50])
        if en['idx'] >= body_start_idx and body_page is None and en['bm'] in found:
            body_page = found[en['bm']]
    def roman(n):
        vals = [(10, 'x'), (9, 'ix'), (5, 'v'), (4, 'iv'), (1, 'i')]; s = ''
        for v, r in vals:
            while n >= v: s += r; n -= v
        return s
    labels = {}
    for bm, p in found.items():
        labels[bm] = str(p - body_page + 1) if p >= body_page else roman(p - toc_page + 1)
    return labels, len(pages), toc_page, body_page


def main(path):
    with zipfile.ZipFile(path) as z:
        files = {n: z.read(n) for n in z.namelist()}
    x = files['word/document.xml'].decode('utf8')
    head, els, sect, tail = split_body(x)
    if 'w:name="_Toc5' in x:
        sys.exit('Already finalized; rebuild the docx first.')
    body_start = next(i for i, e in enumerate(els) if style_of(e) == '47' and text_of(e).strip() == 'LỜI MỞ ĐẦU')
    entries = collect(els, body_start)
    add_bookmarks(els, entries)
    fields = find_field_paras(els)

    def write(doc_xml):
        files['word/document.xml'] = doc_xml.encode('utf8')
        with zipfile.ZipFile(path, 'w', zipfile.ZIP_DEFLATED) as z:
            for n, data in files.items():
                z.writestr(n, data)

    # pass 1: placeholder page numbers so front-matter layout is final
    write(assemble(head, els, sect, tail, fields, build_lists(entries, {e['bm']: '0' for e in entries}), body_start))
    pages, total, toc_page, body_page = render_pages(path, entries, body_start)
    # pass 2: real numbers
    write(assemble(head, els, sect, tail, fields, build_lists(entries, pages), body_start))
    pages2, total, toc_page, body_page = render_pages(path, entries, body_start)
    if pages2 != pages:
        write(assemble(head, els, sect, tail, fields, build_lists(entries, pages2), body_start))
    print('Total PDF pages: %d | front matter starts p.%d | body (LỜI MỞ ĐẦU -> end): %d pages' % (total - 1, toc_page + 1, total - 1 - body_page))
    print('PDF preview:', path[:-5] + '.pdf')


if __name__ == '__main__':
    main(sys.argv[1])
