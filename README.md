# पैकिंग सूची · Packing Checklist · यात्रासामग्रीसूची

Version 1.1.0 is an offline Android packing checklist with Hindi, English and Sanskrit interfaces.
Targets Android 15 (API 35); runs on Android 7.0 (API 24) and later.

## What is new

- Create, rename and delete categories. Removing a category moves its items and notes to Other.
- Add and edit each item, with an optional multiline note and optional names in all three languages.
- Switch the entire interface and the original 27 item names between Hindi, English and Sanskrit.
- Select day mode, night mode, or follow the device theme. Language and theme are saved.
- Search all three names, notes and category names; filter by category and packed status.
- Undo an accidental item deletion while preserving its note, checked status and position.
- Export a JSON backup and restore it after confirmation, including categories, items, notes and settings.
- Use a new suitcase/checkmark launcher icon, including adaptive and monochrome variants.

User-written names and notes are not automatically translated. If a name is missing in the
selected language, another available name is displayed. The built-in list has all three translations.

## Download and install

Download `HindiPackingChecklist-debug.apk` from the newest
[verified preview release](https://github.com/umeshvaidik-ops/HindiPackingChecklist/releases).
The full Android Studio project ZIP and SHA-256 checksums are included in the same release.

These APKs use development signing keys. Builds from different runners can have different
keys, so Android may refuse to update an installed preview. Export a backup before uninstalling
an existing 1.1 preview, then restore it after reinstalling. Version 1.0 does not have backup export:
record its custom checklist before uninstalling it. Uninstalling can erase locally saved data.
For updates installed with the same signing key, the app migrates version 1.0 data automatically,
retaining checked items, custom text, deleted items and empty lists.

## Using the app

- Tap **+ Add item** to add a name, select a category, and add a note.
- Create a category inside the item editor or through **Categories** on the main screen.
- Tap a pencil to edit an item or category. Notes remain visible beneath item names.
- Open **Settings** (gear) for language, appearance and backup/restore.
- The three-dot menu can uncheck all items or restore the original list. Restoring defaults
  requires confirmation and removes custom entries, categories and notes; language and theme stay.

The app requests no internet or broad file-storage permission. Backup files are selected through
Android's document picker. Store personal backup files where you consider appropriate.

## Android Studio and command line

1. Extract the source ZIP and open its `HindiPackingChecklist` folder.
2. Select Java 17 as the Gradle JDK.
3. Install Android SDK Platform 35 and Build-Tools 35.0.0, then sync and run `app`.

The full Gradle wrapper is included. Android Studio supplies the local SDK path; do not commit
machine-specific `local.properties` files.

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Use `gradlew.bat` on Windows. Output: `app/build/outputs/apk/debug/app-debug.apk`.

## Verification

Robolectric data tests cover version 1 migration, custom categories, item edits, notes, language
and theme persistence, search, category reassignment, duplicate IDs, invalid and oversized backups,
and backup round trips. Compose UI tests exercise all three languages, day/night modes, new
categories, notes, editing, search, checked state, activity recreation and undo deletion.
Rendered screenshots and test/lint reports are uploaded as GitHub Actions artifacts.
Physical-device testing remains necessary before commercial release.

## Build and distribution

Java 17, Gradle 8.10.2, Android Gradle Plugin 8.7.3, Kotlin 2.0.21, Compose BOM 2024.12.01.
GitHub Actions publishes a preview only after the build, data/UI tests and lint succeed on main.
Production signing keys are not part of this project. A Google Play launch additionally requires
release signing, an Android App Bundle and an update to the then-current target SDK requirement.
