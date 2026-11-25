# Agent guide

This repository builds the Android application only. Keep changes scoped to Android and the shared code required by Android.

## Modules

| Module | Purpose |
| --- | --- |
| `androidApp` | Android UI, lifecycle, services and playback integration. |
| `shared` | Common UI and business logic, Android platform code, and JVM-compatible networking/playback code used by Android. |
| `listenTogetherProto` | Protobuf models required by shared networking code. |
| `opusDecoder` | Vendored Media3/libopus JNI decoder used by Android. |

## Implementation rules

- Read the implementation and all callers before changing behavior.
- Preserve existing behavior outside the requested scope.
- Keep reusable business logic in `shared/src/commonMain`; use `shared/src/androidMain` for Android APIs.
- Keep JVM-only APIs needed by Android in `shared/src/jvmSharedMain`.
- Put user-facing strings in `shared/src/commonMain/composeResources/values/strings.xml` and use generated resource accessors.
- Derive locale from system settings or user preferences. Do not hardcode language or country fallbacks.
- Match the surrounding Kotlin and Compose style. Add tests for changed behavior where practical.
- Do not bump `appVersion` unless release work explicitly requires it.

## Build and verification

Run:

```bash
./gradlew :androidApp:compileDebugKotlin
./gradlew :androidApp:assembleDebug
```

Shared Android host tests can be run with the corresponding `:shared:testAndroidHostTest` task.
