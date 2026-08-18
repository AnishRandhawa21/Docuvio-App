# Implementation Plan: Android Release Build Optimization

This plan outlines the steps to optimize the Docuvio app's release build based on Play Console recommendations, focusing on R8 minification, resource shrinking, and build configuration improvements while ensuring zero change in app behavior.

## User Review Required

> [!IMPORTANT]
> **AGP 9.0 Upgrade Deferred**: I have decided to defer the AGP 9.0 upgrade. AGP 9.0 (and the required Gradle 9.1+) introduces "Built-in Kotlin" which replaces the `kotlin-android` plugin. This is a major architectural change that might break existing build logic or third-party plugins (like Razorpay or Firebase) if they are not yet fully compatible with the new DSL.
> I will achieve high optimization scores using the current AGP 8.6.1 by enabling R8 and resource shrinking, which are the primary drivers of the Play Console's "LOW" optimization rating.

> [!NOTE]
> **ABI Filtering**: I will retain `x86` and `x86_64` ABIs to ensure compatibility with all currently supported devices, including ChromeOS and older Intel-based devices, as per the strict constraints.

## Proposed Changes

### Build Configuration

#### [MODIFY] [app/build.gradle.kts](file:///E:/Android-Projects/Docuvio-App/app/build.gradle.kts)
- Enable `isMinifyEnabled = true` for the `release` build type.
- Enable `isShrinkResources = true` for the `release` build type.
- Ensure `proguard-android-optimize.txt` is used for more aggressive R8 optimizations.
- Add `ndk.abiFilters` to ensure 16KB page size compatibility is explicitly handled if necessary (though AGP 8.6+ handles it well).

### R8 / ProGuard Configuration

#### [MODIFY] [app/proguard-rules.pro](file:///E:/Android-Projects/Docuvio-App/app/proguard-rules.pro)
- Add rules for **Retrofit**: Preserve interface methods and generic signatures.
- Add rules for **Gson**: Preserve fields annotated with `@SerializedName`.
- Add rules for **Kotlin Serialization**: Preserve `@Serializable` classes and their companion objects.
- Add rules for **Razorpay**: Ensure payment SDK classes are not obfuscated/shrunk.
- Add rules for **Supabase/Ktor**: Preserve necessary classes for networking and realtime.

### Project-wide Settings

#### [MODIFY] [gradle.properties](file:///E:/Android-Projects/Docuvio-App/gradle.properties) (or create if missing)
- Ensure `android.enableR8.fullMode=true` is set (it's the default in AGP 8+, but good to be explicit for optimization).

---

## Verification Plan

### Automated Tests
- Run `./gradlew clean`
- Run `./gradlew assembleRelease` to verify APK generation.
- Run `./gradlew bundleRelease` to verify AAB generation.
- Run existing unit tests: `./gradlew test`
- Run existing instrumentation tests (if any): `./gradlew connectedAndroidTest`

### Manual Verification
- Inspect the generated AAB using the **APK Analyzer** in Android Studio to confirm:
    - Code shrinking actually happened (compare DEX size).
    - Resource shrinking actually happened.
    - Obfuscation is applied (check class names).
- Verify 16KB page alignment using `readelf` or similar tool if available (or trust AGP 8.6.1 default).
- **CRITICAL**: The user should manually verify the `release` build on a device to ensure:
    - Login/Authentication (Supabase) still works.
    - Payments (Razorpay) still initialize.
    - Camera/ML Kit functionality is intact.
    - Notifications (FCM) are received.
