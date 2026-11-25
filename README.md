# Metrolist Android

A minimal Android client for YouTube song search, playback and lyrics, built with Kotlin and Jetpack Compose.

The application exposes only song search, basic playback controls and the lyrics view. Desktop, iOS, library management, playlists, podcasts, social features, integrations and other secondary experiences are not part of this client.

## Build

Requirements: JDK 25 and an installed Android SDK.

```bash
./gradlew :androidApp:assembleDebug
```

The APK is written to `androidApp/build/outputs/apk/debug/`.

## Project structure

- `androidApp`: Android application, UI, services and platform integration.
- `shared/src/commonMain`: shared UI, models, repositories and business logic used by Android.
- `shared/src/androidMain`: Android platform implementations.
- `shared/src/jvmSharedMain`: JVM-compatible networking and playback code required by Android.
- `listenTogetherProto`: protobuf types consumed by shared networking.
- `opusDecoder`: Android Opus decoder integration.

Only the Android application target is built. Common and JVM-shared source sets remain because Android depends on them.
