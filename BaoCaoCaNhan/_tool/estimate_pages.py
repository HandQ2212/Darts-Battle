#!/usr/bin/env python3
"""Estimate body page count (from LỜI MỞ ĐẦU to end) of a built report .docx.
Heuristic for A4, TNR 13, margins of the template. Usage: python3 estimate_pages.py file.docx"""
import re, sys, zipfile, html, math, os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from docxsplit import split_body
PAGE_H = 728.0          # usable height pt
LINE = 17.2             # body line height pt
CPL = 80                # chars per full line body text
def txt(e): return html.unescape(''.join(re.findall(r'<w:t[^>]*>([^<]*)</w:t>', e)))
def est(e):
    if e.startswith('<w:tbl'):
        h = 0
        grid = [int(g) for g in re.findall(r'<w:gridCol w:w="(\d+)"', e)] or [9400]
        for tr in re.findall(r'<w:tr[ >].*?</w:tr>', e, re.S):
            cells = re.findall(r'<w:tc>.*?</w:tc>', tr, re.S)
            rowl = 1
            for ci, c in enumerate(cells):
                w = grid[ci] if ci < len(grid) else 2000
                cpl = max(8, w / 9400 * 92)
                ps = re.findall(r'<w:p[ >].*?</w:p>', c, re.S)
                rowl = max(rowl, sum(max(1, math.ceil(len(txt(p)) / cpl)) for p in ps))
            h += rowl * 14.2 + 4
        return h + 6
    m = re.search(r'<wp:extent cx="(\d+)" cy="(\d+)"', e)
    if m: return int(m.group(2)) / 12700 + 8
    if 'w:type="page"' in e: return -1
    st = re.search(r'<w:pStyle w:val="(\d+)"', e); st = st.group(1) if st else ''
    t = txt(e)
    if st == '40': return 60
    if st == '47': return 50
    if st in ('41', '43', '45'): return 26
    if st in ('57', '51'): return 26
    n = len(t)
    if st == '30': lines = max(1, math.ceil(n / (CPL - 5)))
    elif 'Courier' in e: lines = max(1, math.ceil(n / 70)); return lines * 13
    else: lines = max(1, math.ceil((n + 5) / CPL))
    return lines * LINE + 6
def main(p):
    x = zipfile.ZipFile(p).read('word/document.xml').decode('utf8')
    _, els, _, _ = split_body(x)
    start = next(i for i, e in enumerate(els) if txt(e).strip() == 'LỜI MỞ ĐẦU')
    pages, used = 1, 0.0
    for e in els[start:]:
        h = est(e)
        if h < 0: pages += 1; used = 0; continue
        if used + h > PAGE_H:
            pages += 1; used = h if h < PAGE_H else 0
        else: used += h
    total = pages - 1 + used / PAGE_H
    print('Estimated body pages (LỜI MỞ ĐẦU -> end): %.1f' % total)
main(sys.argv[1])
