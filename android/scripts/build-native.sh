#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
OUT="$ROOT/android/app/src/main/jniLibs/arm64-v8a"
mkdir -p "$OUT"

command -v cargo-ndk >/dev/null || cargo install cargo-ndk --locked
rustup target add aarch64-linux-android
(
  cd "$ROOT/aether"
  cargo ndk -t arm64-v8a -o "$ROOT/android/app/src/main/jniLibs" build --release --lib
)

test -s "$OUT/libaether.so"
echo "Native Aether core: $OUT/libaether.so"
