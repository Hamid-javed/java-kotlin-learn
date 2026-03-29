# Phase 1 Changelog - Non-AI Feature Implementation

**Date:** 2026-03-29
**Scope:** All non-AI features from WPI_App_Requirements.docx

---

## Bug Fixes

### #26 - Timestamps Not Saving on New WPI Creation (HIGH)
**Problem:** When a user set Encountered or Viewed counts while creating a new WPI, the counts were saved but the corresponding timestamps (`readCountUpdatedAt`, `displayCountUpdatedAt`, `firstEncounteredAt`, `firstViewedAt`) were not set.

**Fix:** Updated `WIPRepository.insertWIP()` to automatically set timestamps when counts are non-zero during creation.

**File changed:** `data/repository/WIPRepository.kt`

---

### #34 - Allow Dot (.) Character in Source Entry Editing (LOW)
**Problem:** The edit dialog for source entries blocked the dot (`.`) character, making it impossible to edit source URLs like `bbc.com`.

**Fix:** Removed the dot restriction from the `InputFilter` in the source edit dialog. The main input field was already fixed; only the edit dialog still had the old filter.

**File changed:** `fragments/SourceSelectionBottomSheetFragment.kt`

---

## Flashcard / Carousel Improvements

### #6 - Randomize Flashcard Sequence (HIGH PRIORITY)
**Requirement:** After applying filters, the flashcard list should be randomizable.

**Implementation:** Added a **Shuffle** button in the carousel navigation bar. When tapped, the current card list is shuffled into a new random order and the view resets to card 1. A "Cards shuffled" toast confirms the action.

**Files changed:** `fragments/CarouselFragment.kt`, `res/layout/fragment_carousel.xml`

---

### #7 - Show Current Card Number (HIGH)
**Requirement:** Show the current position out of total (e.g., "Card 7 of 42").

**Implementation:** Added a `tvCardPosition` TextView centered in the top bar that displays "Card X of Y". Updates automatically on every scroll/snap event and on initial load.

**Files changed:** `fragments/CarouselFragment.kt`, `res/layout/fragment_carousel.xml`

---

### #8 - Direct Navigation to Card Number (HIGH)
**Requirement:** User should be able to type a card number and jump directly to that card.

**Implementation:** Added an `EditText` input field in the navigation bar with a Go button. User types a card number (1-based) and taps Go or presses the keyboard action button. Validates the input is within range (1 to total cards) and shows an error toast if not.

**Files changed:** `fragments/CarouselFragment.kt`, `res/layout/fragment_carousel.xml`

---

### #9 - Music-Player Style Navigation Buttons (HIGH)
**Requirement:** Add navigation buttons: Forward, Backward, Jump to Start, Jump to End.

**Implementation:** Added four `ImageButton` controls in the carousel navigation bar:
- **First** (skip to beginning) - jumps to card 1
- **Previous** (rewind) - goes back one card
- **Next** (fast forward) - goes forward one card
- **Last** (skip to end) - jumps to the last card

All buttons use smooth scrolling for visual feedback.

**Files changed:** `fragments/CarouselFragment.kt`, `res/layout/fragment_carousel.xml`

---

### #10 - Pause Button for Auto Scroll (HIGH)
**Requirement:** Add a Pause button during auto-scroll so user can pause and resume.

**Implementation:** Added a **Pause/Play** toggle button in the navigation bar. Only visible when auto-scroll is enabled. When paused:
- Auto-scroll handler stops posting scroll events
- Button icon changes from pause to play
- Content description updates for accessibility

Tapping again resumes auto-scroll from the current position.

**Files changed:** `fragments/CarouselFragment.kt`, `res/layout/fragment_carousel.xml`, `viewmodel/SharedViewModel.kt`

---

### #11 - No Restart After Auto Scroll Completes (MEDIUM)
**Requirement:** When auto-scroll reaches the last card, it should stop, not loop back.

**Implementation:** Changed the auto-scroll `Runnable` logic. Previously, when `currentPosition >= totalItems - 1`, it would reset to position 0. Now it sets `isAutoScrollPaused = true` and stops the handler, effectively pausing at the last card. User can manually navigate back or resume.

**File changed:** `fragments/CarouselFragment.kt`

---

### #12 - Allow Any Positive Value for Auto Scroll Interval (LOW)
**Requirement:** The auto-scroll interval should accept any positive number, not just 5-60.

**Implementation:** Removed the `seconds in 5..60` validation. Now accepts any positive integer. Changed the input field `maxLength` from 2 to 4 to allow larger values (up to 9999 seconds). Error message updated to "Interval must be a positive number".

**Files changed:** `fragments/WIPFilterFragment.kt`, `res/layout/fragment_w_i_p_filter.xml`

---

### #13 - Option to Control Count/Timestamp Update During Flashcard (MEDIUM)
**Requirement:** Add checkboxes letting user choose whether View Counts and Timestamps should be updated while flashcards are displayed.

**Implementation:** Added two checkboxes in the Filter screen under a new "Flashcard Options" section:
- **"Update View Counts during flashcard"** - checked by default. When unchecked, the carousel will NOT increment `displayCount` as cards are viewed.
- **"Update Timestamps during flashcard"** - checked by default. Controls whether timestamps are updated.

State is stored in `SharedViewModel` and read by `CarouselFragment` in the scroll listener.

**Files changed:** `fragments/WIPFilterFragment.kt`, `fragments/CarouselFragment.kt`, `viewmodel/SharedViewModel.kt`, `res/layout/fragment_w_i_p_filter.xml`

---

## Filter Screen Improvements

### #14 - Sort Filter Results (HIGH)
**Requirement:** Allow user to sort filter results by various timestamps.

**Implementation:** Added a "Sort results by" dropdown spinner at the bottom of the filter screen with options:
- (none) - default, no sorting
- **Last Viewed** - sorts by `displayCountUpdatedAt` descending
- **Last Encountered** - sorts by `readCountUpdatedAt` descending
- **Created** - sorts by `createdAt` descending
- **Para Created** - sorts by `lastParaCreatedAt` descending

Sorting is applied to filtered results before they are stored in `SharedViewModel` and passed to the carousel or list.

**Files changed:** `fragments/WIPFilterFragment.kt`, `viewmodel/SharedViewModel.kt`, `res/layout/fragment_w_i_p_filter.xml`

---

### #15 - 'NOT' Filter Option (MEDIUM)
**Requirement:** For each filter operator, add a NOT option (e.g., NOT equal to).

**Implementation:** Added `!=` (not equal) operator to all filter spinners (read count, viewed count, and all timestamp filters). For counts, `!=` returns items where the value does not equal the entered number. For dates, `!=` returns items where the timestamp is not within the same minute as the selected date/time.

**File changed:** `fragments/WIPFilterFragment.kt`

---

### #16 - Null / Empty Value Filter (MEDIUM)
**Requirement:** Allow filtering WPIs where a particular field is empty or has no value.

**Implementation:** Added two new operators to all filter spinners:
- **`null`** - matches items where the field is null, zero, or empty
- **`!null`** - matches items where the field has a non-zero/non-null value

For counts: `null` matches where count is null or 0. For timestamps: `null` matches where timestamp is 0 (never set).

These operators do not require entering a value in the input field.

**File changed:** `fragments/WIPFilterFragment.kt`

---

### #17 - Separate 'Show Filter Results' and 'Start Flashcards' Buttons (MEDIUM)
**Requirement:** Split into two distinct buttons so user can view results as a list OR start flashcards separately.

**Implementation:** The bottom button bar now has three buttons:
- **"Clear"** - clears all filters (previously "Clear Filters")
- **"Show List"** - applies filters and navigates back to `WIPListFragment` showing only filtered results
- **"Flashcards"** - applies filters and navigates to `CarouselFragment` as before (previously "Apply Filter")

The "Show List" button sets `isFilterApplied = true` and stores filtered results in `SharedViewModel.filteredWipsList`, then pops back to the list screen which detects and displays the filtered list.

**Files changed:** `fragments/WIPFilterFragment.kt`, `fragments/WIPListFragment.kt`, `res/layout/fragment_w_i_p_filter.xml`

---

## WPI Management

### #18 - Duplicate Detection While Adding (HIGH)
**Requirement:** When user types a new WPI, app should show a dropdown of matching existing WPIs so user is warned before creating a duplicate.

**Implementation:**
1. Changed the Word input field from `TextInputEditText` to `AutoCompleteTextView` with `completionThreshold="2"` (dropdown appears after 2 characters)
2. Populated the autocomplete adapter with all existing WPI words from the database
3. Added a red warning `TextView` below the word field that appears when typing matches existing entries: "Possible duplicate: X existing WPI(s) match"
4. Warning only appears when adding a new WPI (not when editing an existing one)

**Files changed:** `fragments/WIPEditFragment.kt`, `res/layout/fragment_w_i_p_edit.xml`

---

## Tag Management

### #32 - Delete / Reset All Tags (LOW)
**Requirement:** Add an option to delete or reset all tags from all WPIs at once.

**Implementation:** Added a **"Delete All Tags"** option (in red text) to the 3-dot menu dialog on the main list screen. When tapped:
1. A confirmation dialog appears: "This will remove ALL tags from ALL WPIs. This cannot be undone."
2. On confirm, fetches all unique tags via `wipViewModel.allTags` and calls `removeTagsFromAllWIPs()` to strip them from every WPI
3. Shows "All tags deleted" toast on completion
4. Refreshes the list

**Files changed:** `fragments/WIPListFragment.kt`, `res/layout/custom_dialog_layout.xml`

---

## Summary of All Files Changed

| File | Changes |
|------|---------|
| `data/repository/WIPRepository.kt` | Bug fix: timestamps on new WPI creation |
| `viewmodel/SharedViewModel.kt` | New state: `isAutoScrollPaused`, `updateViewCountDuringFlashcard`, `updateTimestampsDuringFlashcard`, `sortBy` |
| `fragments/CarouselFragment.kt` | Full rewrite: shuffle, card counter, navigation buttons, jump-to-card, pause, stop-at-end, count update control |
| `fragments/WIPFilterFragment.kt` | Sort spinner, flashcard option checkboxes, NOT/null operators, separate buttons, any-positive interval |
| `fragments/WIPEditFragment.kt` | Duplicate detection with AutoCompleteTextView and warning label |
| `fragments/WIPListFragment.kt` | Show filtered list from filter, delete all tags menu option |
| `fragments/SourceSelectionBottomSheetFragment.kt` | Allow dot in edit dialog |
| `res/layout/fragment_carousel.xml` | Navigation bar, card position, shuffle/pause buttons |
| `res/layout/fragment_w_i_p_filter.xml` | Flashcard options checkboxes, sort spinner, 3-button layout |
| `res/layout/fragment_w_i_p_edit.xml` | AutoCompleteTextView for word, duplicate warning label |
| `res/layout/custom_dialog_layout.xml` | "Delete All Tags" menu option |
| `DOCUMENTATION.md` | Updated feature descriptions |
| `CLAUDE.md` | Updated architecture notes |

---

## Phase 2 (Planned - AI Features)

The following features are deferred to Phase 2:

| # | Feature | Category |
|---|---------|----------|
| 1 | Show source webpage link for AI content | AI |
| 2 | Different topic on each Regenerate click | AI |
| 3 | Fetch Meaning via AI button | AI |
| 4 | Show similar WPIs using AI | AI |
| 5 | Positive/Negative connotation via AI | AI |
| 27 | Export to Google Drive | Export/Import |
