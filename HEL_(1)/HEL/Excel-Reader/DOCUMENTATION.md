# HEL - Excel Reader (Vocabulary Learning App)

**Package:** `com.rameez.hel.v2`
**Version:** 2.0
**Language:** Kotlin
**Min SDK:** 26 (Android 8.0) | **Target SDK:** 34 (Android 14)

---

## Table of Contents

1. [Overview](#1-overview)
2. [Features](#2-features)
3. [Project Structure](#3-project-structure)
4. [Architecture](#4-architecture)
5. [Database Schema](#5-database-schema)
6. [Screens & Navigation](#6-screens--navigation)
7. [Key Workflows](#7-key-workflows)
8. [Dependencies](#8-dependencies)
9. [Permissions](#9-permissions)
10. [How to Build & Run](#10-how-to-build--run)

---

## 1. Overview

HEL (Excel Reader) is a vocabulary learning and Word-In-Progress (WIP) management Android application. It allows users to:

- Import vocabulary words from Excel (.xlsx) files
- Track reading and viewing statistics for each word
- Generate AI-powered sentences and articles using selected news sources
- Browse words in a carousel/flashcard format with text-to-speech
- Filter and search words with advanced criteria

---

## 2. Features

### Core Features
- **Excel Import** - Import vocabulary from `.xlsx` files using Apache POI
- **Word Management** - Add, edit, delete, and organize vocabulary words
- **Custom Tags** - Tag words with user-defined labels for organization
- **Category System** - Group words by category (e.g., Noun, Verb, Adjective)

### Learning Features
- **Carousel Mode** - Flashcard-style learning with one word per screen
- **Card Position Indicator** - Shows "Card X of Y" during flashcard display
- **Music-Player Navigation** - First, Previous, Next, Last buttons for card navigation
- **Jump to Card** - Type a card number to navigate directly to it
- **Shuffle/Randomize** - Randomize card order via a shuffle button
- **Text-to-Speech (TTS)** - Read words aloud with play/pause/stop controls
- **Auto-Scroll** - Automatically advance through words at any positive interval
- **Pause/Resume Auto-Scroll** - Pause and resume auto-scroll during flashcard display
- **Auto-Scroll Stops at End** - Auto-scroll stops at the last card instead of looping
- **Count/Timestamp Update Control** - Checkboxes to control whether view counts and timestamps update during flashcard display
- **Timer** - Set time limits for study sessions
- **View/Read Count Tracking** - Track how many times each word has been viewed and encountered

### AI Features
- **AI Sentence Generation** - Generate contextual sentences for individual words
- **AI Article Generation** - Generate paragraphs using multiple selected words
- **News Source Selection** - Choose which news sources the AI references
- **Source URL Linking** - Generated sentences include source references

### Search & Filter
- **Real-time Search** - Search by word or meaning
- **Advanced Filters**:
  - By category (multi-select)
  - By custom tags (multi-select)
  - By read/view count (with operators: =, <, >, <=, >=, <>, !=, null, !null)
  - By text content (word, meaning, sample sentence)
  - By timestamps (created, modified, first viewed, first encountered, last encountered, article creation)
  - NOT operators (!=) and null/empty value filters for all filter fields
  - By timer duration
- **Sort Results** - Sort filtered results by Last Viewed, Last Encountered, Created, or Para Created timestamp
- **Separate Actions** - "Show List" to view filtered results as a list, "Flashcards" to start flashcard display
- **Duplicate Detection** - Warning shown when adding a WPI that matches existing entries

### Tag Management
- **Tag Suggestions** - Autocomplete dropdown when adding tags
- **Selective Tag Removal** - Remove specific tags via checkbox dialog
- **Delete All Tags** - One-click option to remove all tags from all WPIs

### Data Tracking
- **Creation timestamp** - When the word was imported/added
- **Modification timestamp** - When the word content was last changed
- **First viewed / First encountered** - When the word was first seen
- **Last viewed / Last encountered** - Most recent interaction
- **Last article creation** - When the word was last used in an AI article

---

## 3. Project Structure

```
Excel-Reader/
├── app/
│   ├── build.gradle.kts              # App-level build config
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/rameez/hel/
│       │   ├── MainActivity.kt        # Single activity host
│       │   ├── SharedPref.kt          # SharedPreferences wrapper
│       │   ├── adapter/
│       │   │   ├── CarouselAdapter.kt
│       │   │   ├── CategoryAdapter.kt
│       │   │   ├── CustomTagsAdapter.kt
│       │   │   ├── DeleteTagsAdapter.kt
│       │   │   ├── SourceAdapter.kt
│       │   │   ├── WIPListAdapter.kt
│       │   │   └── WIPSearchAdapter.kt
│       │   ├── data/
│       │   │   ├── ArticleModel.kt     # Generated article entity
│       │   │   ├── Converters.kt       # Room type converters
│       │   │   ├── CustomTagsModel.kt
│       │   │   ├── SourceModel.kt
│       │   │   ├── WIPDao.kt           # Data Access Object
│       │   │   ├── WIPDatabase.kt      # Room database
│       │   │   ├── WIPModel.kt         # Core vocabulary entity
│       │   │   └── repository/
│       │   │       └── WIPRepository.kt
│       │   ├── fragments/
│       │   │   ├── ArticleBottomSheetFragment.kt
│       │   │   ├── CarouselFragment.kt
│       │   │   ├── DeleteTagsFragment.kt
│       │   │   ├── SourceSelectionBottomSheetFragment.kt
│       │   │   ├── SplashFragment.kt
│       │   │   ├── WIPDetailFragment.kt
│       │   │   ├── WIPEditFragment.kt
│       │   │   ├── WIPFilterFragment.kt
│       │   │   ├── WIPListFragment.kt
│       │   │   └── WIPSearchFragment.kt
│       │   ├── utils/
│       │   │   ├── AiSentenceService.kt
│       │   │   ├── ApplicationClass.kt
│       │   │   ├── Migration.kt
│       │   │   ├── PermissionUtils.kt
│       │   │   ├── TimePicker.kt
│       │   │   └── checkInternetConnection.kt
│       │   └── viewmodel/
│       │       ├── SharedViewModel.kt
│       │       └── WIPViewModel.kt
│       └── res/
│           ├── layout/                 # All XML layouts
│           ├── navigation/             # Navigation graph
│           ├── drawable/               # Icons & shapes
│           ├── font/                   # Poppins font family
│           ├── values/                 # Colors, strings, themes
│           └── values-night/           # Dark theme
├── build.gradle.kts                    # Root build config
├── settings.gradle.kts
├── gradle.properties
└── gradlew / gradlew.bat
```

---

## 4. Architecture

The app follows the **MVVM (Model-View-ViewModel)** pattern with a **Repository** layer.

```
┌─────────────────────────────────────────────────┐
│                   UI Layer                       │
│  Fragments / Adapters / BottomSheets             │
│  (Observes LiveData, sends user actions)         │
└──────────────────────┬──────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────┐
│               ViewModel Layer                    │
│  WIPViewModel        SharedViewModel             │
│  (Business logic,    (Temporary UI state:        │
│   coroutines,         filters, selections,       │
│   LiveData)           timer state)               │
└──────────────────────┬──────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────┐
│              Repository Layer                    │
│  WIPRepository                                   │
│  (Timestamp management, business rules,          │
│   abstracts data source)                         │
└──────────────────────┬──────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────┐
│                Data Layer                        │
│  Room Database (WIPDao)    AiSentenceService     │
│  SharedPref                (Google Apps Script)   │
└─────────────────────────────────────────────────┘
```

### Key Components

| Component | Role |
|-----------|------|
| **WIPViewModel** | Orchestrates all WIP/Article CRUD, AI generation, count tracking via coroutines |
| **SharedViewModel** | Holds transient UI state shared across fragments (filters, timer, selection sets) |
| **WIPRepository** | Wraps DAO with business logic (smart timestamp updates, first-time tracking) |
| **WIPDao** | Room DAO with all SQL queries |
| **WIPDatabase** | Room database singleton (version 6, with 5 migrations) |
| **AiSentenceService** | HTTP client for Google Apps Script AI integration |

---

## 5. Database Schema

### WIP_LIST Table (WIPModel)

| Column | Type | Description |
|--------|------|-------------|
| `id` | INTEGER (PK) | Auto-generated primary key |
| `sr` | REAL | Serial number from Excel import |
| `category` | TEXT | Word category (Noun, Verb, etc.) |
| `wip` | TEXT | The vocabulary word |
| `meaning` | TEXT | Word definition |
| `sampleSentence` | TEXT | Example sentence (may be AI-generated) |
| `customTag` | TEXT | Comma-separated custom tags |
| `readCount` | REAL | Times the word was encountered/marked |
| `displayCount` | REAL | Times the word was viewed |
| `createdAt` | INTEGER | Import/creation timestamp |
| `modifiedAt` | INTEGER | Last content modification timestamp |
| `displayCountUpdatedAt` | INTEGER | Last view count change timestamp |
| `readCountUpdatedAt` | INTEGER | Last read count change timestamp |
| `firstViewedAt` | INTEGER | First time this word was viewed |
| `firstEncounteredAt` | INTEGER | First time this word was encountered |
| `lastParaCreatedAt` | INTEGER | Last time used in article generation |

### GENERATED_ARTICLES Table (ArticleModel)

| Column | Type | Description |
|--------|------|-------------|
| `id` | INTEGER (PK) | Auto-generated primary key |
| `content` | TEXT | Generated article/paragraph text |
| `wipIds` | TEXT | Comma-separated IDs of words used |
| `createdAt` | INTEGER | Article creation timestamp |

### Database Migrations

| Migration | Changes |
|-----------|---------|
| 1 -> 2 | Added timestamp columns (createdAt, modifiedAt, displayCountUpdatedAt, readCountUpdatedAt) |
| 2 -> 3 | Added first encounter tracking (firstViewedAt, firstEncounteredAt) |
| 3 -> 4 | Created GENERATED_ARTICLES table |
| 4 -> 5 | Added wipIds column to GENERATED_ARTICLES |
| 5 -> 6 | Added lastParaCreatedAt column to WIP_LIST |

---

## 6. Screens & Navigation

The app uses **Navigation Component** with a single Activity and multiple Fragments.

```
SplashFragment (start destination)
    │
    ▼
WIPListFragment (main screen)
    ├──► WIPSearchFragment (real-time search)
    │       └──► WIPDetailFragment
    ├──► WIPFilterFragment (advanced filters)
    │       └──► CarouselFragment (flashcard mode)
    ├──► WIPDetailFragment (word details)
    │       ├──► WIPEditFragment (add/edit word)
    │       │       └──► DeleteTagsFragment
    │       └──► SourceSelectionBottomSheetFragment
    └──► ArticleBottomSheetFragment (view generated articles)
```

### Screen Descriptions

| Screen | Purpose |
|--------|---------|
| **SplashFragment** | 1-second splash screen with fullscreen display |
| **WIPListFragment** | Main list of all vocabulary words. Import/export Excel, bulk select, generate articles |
| **WIPDetailFragment** | View a single word's details, counts, timestamps. Generate AI sentence, read aloud, edit/delete |
| **WIPEditFragment** | Add new word or edit existing. Manage tags, set counts, input word/meaning/sentence |
| **WIPFilterFragment** | Advanced multi-criteria filtering. Category, tags, counts, dates, text search, timer |
| **WIPSearchFragment** | Real-time search by word or meaning. Selection mode for bulk article generation |
| **CarouselFragment** | Flashcard mode with PagerSnapHelper. Auto-scroll, TTS, timer, session count tracking |
| **DeleteTagsFragment** | Checkbox UI to select which tags to remove from a word |
| **ArticleBottomSheetFragment** | Display generated articles. Word highlighting, TTS, regenerate, open source links |
| **SourceSelectionBottomSheetFragment** | Manage news sources for AI generation. Add, delete, toggle sources |

### Adapters

| Adapter | Used By | Purpose |
|---------|---------|---------|
| WIPListAdapter | WIPListFragment | Main word list with selection mode |
| WIPSearchAdapter | WIPSearchFragment | Search results with selection mode |
| CarouselAdapter | CarouselFragment | Full-screen flashcard items |
| CategoryAdapter | WIPFilterFragment | Category checkbox grid |
| CustomTagsAdapter | WIPFilterFragment | Tag checkbox grid |
| DeleteTagsAdapter | DeleteTagsFragment | Tag deletion checkboxes |
| SourceAdapter | SourceSelectionBottomSheetFragment | News source list |

---

## 7. Key Workflows

### 7.1 Excel Import

```
1. User taps Import button in WIPListFragment
2. Storage permission check (PermissionUtils)
3. File picker opens -> user selects .xlsx file
4. Apache POI parses the file:
   Column 1: SR (serial number)
   Column 2: Category
   Column 3: WIP (word)
   Column 4: Meaning
   Column 5: Sample Sentence
5. Each row creates a WIPModel -> inserted into database
6. RecyclerView auto-updates via LiveData
```

### 7.2 AI Sentence Generation

```
1. User views a word in WIPDetailFragment
2. Taps "Generate AI Sentence"
3. If no sources saved -> SourceSelectionBottomSheetFragment opens
4. AiSentenceService sends HTTP POST to Google Apps Script:
   - Payload: { word, apiKey, sources }
   - Timeout: 30s connect, 60s read
   - Retries: up to 2 times
5. Response parsed (sentence + source URL + source name)
6. WIP updated with new sampleSentence
7. UI refreshes via LiveData
```

### 7.3 AI Article Generation

```
1. User selects multiple words (CarouselFragment or WIPSearchFragment)
2. Taps "Generate Para"
3. Source selection if needed
4. AiSentenceService.generateNewsArticle() called with word list
5. ArticleModel created with content and wipIds
6. lastParaCreatedAt updated for each involved word
7. ArticleBottomSheetFragment displays the article
```

### 7.4 Carousel / Flashcard Learning

```
1. User navigates to CarouselFragment (with optional filters)
2. PagerSnapHelper shows one word per page
3. View count incremented once per word per session
4. Optional features:
   - Auto-scroll at N-second intervals
   - Text-to-speech reads each word aloud
   - Timer auto-dismisses after set duration
   - Selection mode for bulk article generation
```

### 7.5 Filtering

```
1. User opens WIPFilterFragment
2. Sets any combination of:
   - Categories (multi-select checkboxes)
   - Custom tags (multi-select checkboxes)
   - Read count (value + operator)
   - View count (value + operator)
   - Date ranges (with DatePickerDialog)
   - Text search (word, meaning, sentence)
   - Timer duration
3. Apply -> filtered list stored in SharedViewModel
4. WIPListFragment / CarouselFragment displays filtered results
```

---

## 8. Dependencies

| Library | Version | Purpose |
|---------|---------|---------|
| AndroidX Core KTX | 1.9.0 | Kotlin extensions for Android |
| AppCompat | 1.6.1 | Backward-compatible UI |
| Material Design | 1.11.0 | Material components |
| ConstraintLayout | 2.1.4 | Flexible layouts |
| Navigation Fragment/UI | 2.7.7 | Fragment navigation |
| Room Runtime/KTX | 2.8.4 | Local database |
| Room Compiler (kapt) | 2.8.4 | Annotation processing |
| OkHttp3 | 5.3.2 | HTTP client for AI service |
| Apache POI | 5.2.5 | Excel file reading |
| Apache POI OOXML | 5.2.3 | .xlsx format support |
| Glide | 4.16.0 | Image loading |
| JUnit | 4.13.2 | Unit testing |
| Espresso | 3.5.1 | UI testing |

---

## 9. Permissions

| Permission | Purpose |
|------------|---------|
| `READ_EXTERNAL_STORAGE` | Read Excel files from storage |
| `WRITE_EXTERNAL_STORAGE` | Export data to storage |
| `READ_MEDIA_IMAGES` | Media access (Android 13+) |
| `INTERNET` | AI sentence/article generation via Google Apps Script |
| `ACCESS_NETWORK_STATE` | Check internet connectivity before API calls |

---

## 10. How to Build & Run

### Prerequisites

- **Android Studio** (Arctic Fox or later recommended)
- **JDK 17** (bundled with Android Studio)
- **Android SDK 34** (install via SDK Manager)

### Method 1: Android Studio (GUI)

1. Open Android Studio
2. **File -> Open** -> navigate to `HEL_(1)/HEL/Excel-Reader/`
3. Wait for Gradle sync to complete
4. Set up a device:
   - **Emulator:** Tools -> Device Manager -> Create Virtual Device -> select phone & system image
   - **Physical device:** Enable Developer Options & USB Debugging, connect via USB
5. Select device in the toolbar dropdown
6. Click the green **Run** button (or `Shift+F10`)

### Method 2: Command Line

```bash
# Navigate to the project
cd "HEL_(1)/HEL/Excel-Reader"

# Build debug APK
./gradlew assembleDebug

# Build and install on connected device/emulator
./gradlew installDebug

# Launch the app
adb shell am start -n com.rameez.hel.v2/com.rameez.hel.MainActivity
```

### Useful Commands

| Command | Description |
|---------|-------------|
| `./gradlew assembleDebug` | Build debug APK only |
| `./gradlew installDebug` | Build + install on device |
| `./gradlew assembleRelease` | Build release APK |
| `./gradlew clean` | Clean build artifacts |
| `adb devices` | List connected devices/emulators |
| `adb logcat` | View real-time app logs |
| `adb shell am force-stop com.rameez.hel.v2` | Force stop the app |
| `adb uninstall com.rameez.hel.v2` | Uninstall the app |

### APK Output Location

```
app/build/outputs/apk/debug/app-debug.apk
```

---

## Class Reference

| Category | Class | File |
|----------|-------|------|
| **Activity** | MainActivity | `MainActivity.kt` |
| **Application** | ApplicationClass | `utils/ApplicationClass.kt` |
| **Database** | WIPDatabase | `data/WIPDatabase.kt` |
| **DAO** | WIPDao | `data/WIPDao.kt` |
| **Entities** | WIPModel | `data/WIPModel.kt` |
| | ArticleModel | `data/ArticleModel.kt` |
| **Models** | SourceModel | `data/SourceModel.kt` |
| | CustomTagsModel | `data/CustomTagsModel.kt` |
| **Repository** | WIPRepository | `data/repository/WIPRepository.kt` |
| **ViewModels** | WIPViewModel | `viewmodel/WIPViewModel.kt` |
| | SharedViewModel | `viewmodel/SharedViewModel.kt` |
| **Fragments** | SplashFragment | `fragments/SplashFragment.kt` |
| | WIPListFragment | `fragments/WIPListFragment.kt` |
| | WIPDetailFragment | `fragments/WIPDetailFragment.kt` |
| | WIPEditFragment | `fragments/WIPEditFragment.kt` |
| | WIPFilterFragment | `fragments/WIPFilterFragment.kt` |
| | WIPSearchFragment | `fragments/WIPSearchFragment.kt` |
| | CarouselFragment | `fragments/CarouselFragment.kt` |
| | DeleteTagsFragment | `fragments/DeleteTagsFragment.kt` |
| | ArticleBottomSheetFragment | `fragments/ArticleBottomSheetFragment.kt` |
| | SourceSelectionBottomSheetFragment | `fragments/SourceSelectionBottomSheetFragment.kt` |
| **Adapters** | WIPListAdapter | `adapter/WIPListAdapter.kt` |
| | WIPSearchAdapter | `adapter/WIPSearchAdapter.kt` |
| | CarouselAdapter | `adapter/CarouselAdapter.kt` |
| | CategoryAdapter | `adapter/CategoryAdapter.kt` |
| | CustomTagsAdapter | `adapter/CustomTagsAdapter.kt` |
| | DeleteTagsAdapter | `adapter/DeleteTagsAdapter.kt` |
| | SourceAdapter | `adapter/SourceAdapter.kt` |
| **Services** | AiSentenceService | `utils/AiSentenceService.kt` |
| **Utilities** | SharedPref | `SharedPref.kt` |
| | PermissionUtils | `utils/PermissionUtils.kt` |
| | TimePicker | `utils/TimePicker.kt` |
| | Converters | `data/Converters.kt` |
| | Migration | `utils/Migration.kt` |
| | checkInternetConnection | `utils/checkInternetConnection.kt` |
