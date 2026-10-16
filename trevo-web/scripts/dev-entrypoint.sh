#!/bin/sh

set -eu

LOCK_HASH_FILE="node_modules/.deps-lock-hash"
CURRENT_LOCK_HASH="$(sha256sum package-lock.json | awk '{ print $1 }')"
INSTALLED_LOCK_HASH=""

if [ -f "$LOCK_HASH_FILE" ]; then
  INSTALLED_LOCK_HASH="$(cat "$LOCK_HASH_FILE")"
fi

if [ ! -d node_modules ] || [ "$CURRENT_LOCK_HASH" != "$INSTALLED_LOCK_HASH" ] || ! npm ls --depth=0 >/dev/null 2>&1; then
  npm install
  printf '%s' "$CURRENT_LOCK_HASH" > "$LOCK_HASH_FILE"
fi

exec npm run dev -- --host 0.0.0.0
