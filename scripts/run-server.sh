#!/bin/sh
set -eu
DARTS_PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
if [ -f "$DARTS_PROJECT_DIR/.local/server.env" ]; then
    set -a
    . "$DARTS_PROJECT_DIR/.local/server.env"
    set +a
fi
exec python3 "$DARTS_PROJECT_DIR/scripts/desktop.py" run server
