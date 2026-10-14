# छुट्टियों की पैकिंग सूची

A Hindi vacation packing checklist built with Kotlin, Jetpack Compose, and Material 3.
Targets Android 15 (API 35); runs on Android 7.0 (API 24) and later.

## Features

- 27 starting items across six categories, with Hindi and English names.
- Check off packed items and view animated completion progress.
- Search in Hindi or English; filter all, remaining, or packed items.
- Add custom items, choose a category, and remove unwanted items.
- Save the checklist locally across app restarts without an account or internet permission.
- Uncheck every item while preserving your list, or restore the original list after confirmation.
- Preserve search, filters, and unfinished add-item text across screen rotation.

## Download and install

Open [GitHub Releases](https://github.com/umeshvaidik-ops/HindiPackingChecklist/releases)
and choose a verified preview. Download `HindiPackingChecklist-debug.apk`, open it on
your Android device, and allow installation from your browser or file manager if prompted.
The APK uses a debug signing key and is intended for personal testing.

The release also contains `HindiPackingChecklist_AndroidProject.zip` and SHA-256 checksums.
GitHub Actions keeps the APK, full source ZIP, and verification reports as artifacts too.

## Open in Android Studio

1. Extract the project ZIP and open the `HindiPackingChecklist` folder.
2. Set the Gradle JDK to Java 17 in Android Studio settings.
3. Install Android SDK Platform 35 and Android SDK Build-Tools 35.0.0.
4. Let Gradle sync, then run the `app` configuration on a device or emulator.

The standard Gradle wrapper is included; installing Gradle separately is unnecessary.
Android Studio creates the machine-specific `local.properties` SDK path automatically.
It is deliberately excluded from version control.

## Command-line build and verification

With Java 17 and `ANDROID_HOME` pointing to your Android SDK:

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

On Windows, use `gradlew.bat` in place of `./gradlew`.
The built APK is at `app/build/outputs/apk/debug/app-debug.apk`.

The persistence tests verify defaults, Hindi text, packed-state restoration, an empty
saved checklist, unchecking custom items, resetting, and recovery from malformed saved data.
They run with Robolectric; they do not substitute for UI testing on a physical device.

## Build configuration

Java 17, Gradle 8.10.2, Android Gradle Plugin 8.7.3, Kotlin 2.0.21,
Compose BOM 2024.12.01, compile/target SDK 35.

GitHub Actions installs the required SDK packages, validates the Gradle wrapper,
builds the APK, runs persistence tests and lint, then publishes a preview for successful
main-branch builds. Production signing is intentionally not stored in this repository;
configure your own release keystore before distributing through an app store.
