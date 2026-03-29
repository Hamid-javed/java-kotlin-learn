# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build and install on connected device/emulator
./gradlew installDebug

# Launch the app on device
adb shell am start -n com.rameez.hel.v2/com.rameez.hel.MainActivity

# Run unit tests
./gradlew testDebugUnitTest

# Run a single unit test class
./gradlew testDebugUnitTest --tests "com.rameez.hel.ExampleUnitTest"

# Run instrumented tests (requires device/emulator)
./gradlew connectedDebugAndroidTest

# Clean build
./gradlew clean
```

On Windows use `gradlew.bat` instead of `./gradlew`.

## Architecture

Single-activity MVVM app with Navigation Component. All source is in `app/src/main/java/com/rameez/hel/`.

**Data flow:** Fragments observe `WIPViewModel` LiveData → `WIPRepository` handles business logic and timestamps → `WIPDao` (Room) executes SQL. A separate `SharedViewModel` holds transient UI state (filters, selections, timer) shared across fragments.

**Key architectural decisions:**
- Room database (`WIPDatabase`, version 6) is the single source of truth. 5 incremental migrations in `utils/Migration.kt` — never use `fallbackToDestructiveMigration`.
- `WIPRepository` manages all timestamp logic: it tracks first-viewed/first-encountered automatically and only updates `modifiedAt` when content actually changes.
- AI generation goes through `AiSentenceService` which calls a Google Apps Script endpoint via OkHttp. It has retry logic (2 attempts) and 30s/60s timeouts.
- Excel import uses Apache POI to parse `.xlsx` files with a fixed column order: SR, Category, WIP, Meaning, Sample Sentence.
- `SharedPref` stores app launch state and news sources as JSON.
- `Converters.kt` handles Room type conversion for `List<String>` and `List<Int>` (comma-separated).

**Navigation:** `nav_graph.xml` starts at `SplashFragment` → `WIPListFragment` (main hub) → branches to Detail, Edit, Search, Filter, Carousel, and DeleteTags fragments. `ArticleBottomSheetFragment` and `SourceSelectionBottomSheetFragment` are shown as bottom sheets, not in the nav graph.

**SharedViewModel state:** Controls flashcard behavior via `updateViewCountDuringFlashcard`, `updateTimestampsDuringFlashcard`, `isAutoScrollPaused`, and `sortBy`. These are set from `WIPFilterFragment` and consumed by `CarouselFragment`.

**Filter operators:** All count and timestamp filters support: `=`, `<`, `>`, `<=`, `>=`, `<>` (between), `!=` (not equal), `null` (empty/zero), `!null` (has value). The `matchDateOperator` helper in `WIPFilterFragment` handles all date comparisons.

## Key Identifiers

- **Package namespace:** `com.rameez.hel`
- **Application ID:** `com.rameez.hel.v2`
- **Min SDK:** 26 | **Target/Compile SDK:** 34
- **JVM target:** 17
- **Kotlin KAPT** used for Room annotation processing

## Database

Two Room entities: `WIPModel` (table `WIP_LIST`) for vocabulary words and `ArticleModel` (table `GENERATED_ARTICLES`) for AI-generated paragraphs. When adding columns, create a new migration in `utils/Migration.kt` and bump the version in `WIPDatabase.kt`.
