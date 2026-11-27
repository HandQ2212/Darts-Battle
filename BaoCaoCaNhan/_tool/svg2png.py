#!/usr/bin/env python3
"""Render SVG to PNG with macOS QuickLook. Usage: python3 svg2png.py in.svg out.png
SVG must have numeric width/height attributes and a white background rect.
Use font-family="Times New Roman" (Vietnamese OK)."""
import re, sys, subprocess, tempfile, shutil, os
src, dst = sys.argv[1], sys.argv[2]
s = open(src, encoding='utf8').read()
w = float(re.search(r'<svg[^>]*\swidth="([\d.]+)', s).group(1)); h = float(re.search(r'<svg[^>]*\sheight="([\d.]+)', s).group(1))
size = int(max(w, h) * 2)
d = tempfile.mkdtemp()
subprocess.run(['qlmanage', '-t', '-s', str(size), '-o', d, src], capture_output=True)
out = os.path.join(d, os.path.basename(src) + '.png')
if not os.path.exists(out): sys.exit('render failed')
pw, ph = int(round(w * size / max(w, h))), int(round(h * size / max(w, h)))
subprocess.run(['sips', '-c', str(ph), str(pw), '--cropOffset', '0', '0', out, '--out', dst], capture_output=True)
shutil.rmtree(d); print('OK', dst, pw, 'x', ph)
