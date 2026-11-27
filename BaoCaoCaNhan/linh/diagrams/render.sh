#!/bin/zsh
# Render each SVG to PNG at 2x with headless Chrome (QuickLook crops large SVGs).
CH="/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
for f in *.svg; do
  w=$(grep -o '<svg[^>]*width="[0-9.]*"' $f | head -1 | sed 's/.*width="\([0-9.]*\)"/\1/')
  h=$(grep -o '<svg[^>]*height="[0-9.]*"' $f | head -1 | sed 's/.*height="\([0-9.]*\)"/\1/')
  "$CH" --headless=new --disable-gpu --hide-scrollbars --force-device-scale-factor=2 --window-size=${w%.*},${h%.*} --screenshot="$PWD/${f%.svg}.png" "file://$PWD/$f" >/dev/null 2>&1
  sips -g pixelWidth -g pixelHeight ${f%.svg}.png | tail -2 | tr '\n' ' '; echo $f
done
