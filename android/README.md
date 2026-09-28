# Aether for Android

A minimal, Persian-first Android VPN client for the Aether core. The home screen has one connect/disconnect button. It uses:

- the existing Rust C API as `libaether.so`;
- Android `VpnService` for a device-wide VPN;
- verified `hev-socks5-tunnel` 2.18.0 to carry the TUN traffic into Aether's local SOCKS5 listener;
- MASQUE and the balanced scan profile by default.

## Supported devices

The first release targets **Android 8+ on arm64-v8a** (nearly all current physical Android phones). The app deliberately excludes its own UID from the VPN, preventing Aether's endpoint sockets from being routed back into the tunnel.

## Build

Requirements: JDK 17, Android SDK 35, NDK 27.2, CMake 3.22, Rust, and `cargo-ndk`.

```bash
./scripts/build-native.sh
./scripts/fetch-tun2socks.sh
gradle assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`. The repository's **Android APK** GitHub Actions workflow performs the same steps and publishes the APK as an artifact; it can also be started manually from the Actions tab.

The tun2socks script pins both the release version and SHA-256. Native `.so` outputs are generated dependencies and do not need to be committed.

## Security notes

- Identity files stay in the app's private storage.
- No SOCKS listener is exposed outside the device loopback interface.
- Uninstalling the app removes its Aether identity.
- This is an initial debug build. A public release should use a private release keystore and complete Android VPN disclosure/privacy requirements.
