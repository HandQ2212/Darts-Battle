#!/bin/sh
set -eu
app=$1
archive=$2
pid=$3
temp=$(dirname "$archive")
backup="${app}.update-backup-$$"
staging="${app}.update-new-$$"
backed_up=0
installed=0
failed() {
  status=$?
  trap - EXIT
  if [ "$status" -ne 0 ]; then
    echo "Update failed (exit $status); attempting to restore the previous app."
    if [ "$backed_up" -eq 1 ]; then
      if [ "$installed" -eq 1 ]; then rm -rf "$app" || true; fi
      mv "$backup" "$app" || echo "Could not restore backup: $backup"
    fi
    rm -rf "$staging" || true
    /usr/bin/osascript -e 'display dialog "Không thể cài bản cập nhật. Bản trước sẽ được mở lại nếu có thể. Xem install.log trong thư mục tạm darts-update để biết chi tiết." buttons {"OK"} default button "OK" with title "Darts Battle"' || true
    if [ -d "$app" ] && ! kill -0 "$pid" 2>/dev/null; then /usr/bin/open "$app" || true; fi
  fi
  exit "$status"
}
trap failed EXIT
tries=0
while kill -0 "$pid" 2>/dev/null; do
  [ "$tries" -lt 60 ] || { echo "The game did not exit in time."; exit 1; }
  tries=$((tries + 1)); sleep 1
done
mkdir "$temp/unpacked"
/usr/bin/ditto -x -k "$archive" "$temp/unpacked"
new="$temp/unpacked/DartsBattle.app"
test -x "$new/Contents/MacOS/DartsBattle"
/usr/bin/codesign --verify --deep --strict "$new"
/usr/bin/ditto "$new" "$staging"
mv "$app" "$backup"
backed_up=1
mv "$staging" "$app"
installed=1
/usr/bin/open "$app"
rm -rf "$backup" || echo "Could not remove backup: $backup"
