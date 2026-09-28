#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
OUT="$ROOT/android/app/src/main/jniLibs/arm64-v8a/libhev-socks5-tunnel.so"
URL="https://github.com/heiher/hev-socks5-tunnel/releases/download/2.18.0/hev-socks5-tunnel-android-arm64-v8a"
SHA256="c72a245d16b68d27407f22b3110811b30cb18ab3b4fecbff026c0652c7f64281"
mkdir -p "$(dirname "$OUT")"
curl -fL --retry 4 --retry-all-errors "$URL" -o "$OUT"
echo "$SHA256  $OUT" | sha256sum -c -
echo "tun2socks: $OUT"
