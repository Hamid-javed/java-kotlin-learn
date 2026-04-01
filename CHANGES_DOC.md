# Changes Documentation

## Files Modified

- `WIPFilterFragment.kt` - Filter logic and operator setup
- `fragment_w_i_p_filter.xml` - Filter page layout
- `fragment_carousel.xml` - Flashcard carousel layout

---

## Issue 1: Sort ASC/DESC Feature

**Status:** Already existed before this change.

**Before:** Sort ASC/DESC toggle button and sort spinner were already implemented with options: Last Viewed, Last Encountered, Created, Para Created.

**After:** No changes needed - feature was already working. The ASC/DESC button visibility was improved (see Issue 4).

---

## Issue 2: Added `!<>` (Not Between) Operator for Counts and Timestamps

**Before:** Count filters (Encountered, Viewed) supported operators: `=, <, >, <=, >=, <>, !=, null, !null`. Timestamp filters (Created At, Modified At) only supported: `=, <, >, <=, >=, <>`. Other timestamp filters (Last Viewed, Last Encountered, First Viewed, First Encountered, Article Created) supported: `=, <, >, <=, >=, <>, !=, null, !null`.

**After:**

- All count filter spinners now include `!<>` (not between) operator: `=, <, >, <=, >=, <>, !<>, !=, null, !null`
- All timestamp filter spinners now include `!<>` and the full operator set: `=, <, >, <=, >=, <>, !<>, !=, null, !null`
- Created At and Modified At spinners were expanded from 6 operators to the full 10-operator set
- `!<>` shows the "To" range field (same as `<>`) and requires both From and To values
- `!<>` returns items NOT within the specified range
- Validation checks (range sanity, required fields) apply to `!<>` just like `<>`

**Changes in `WIPFilterFragment.kt`:**

- `readOperatorSetup()`: Added `"!<>"` to operator list, updated visibility toggle
- `viewedOperatorSetup()`: Added `"!<>"` to operator list, updated visibility toggle
- `createdOperatorSetup()`: Expanded from 6 to 10 operators (added `!<>`, `!=`, `null`, `!null`)
- `modifiedOperatorSetup()`: Expanded from 6 to 10 operators (added `!<>`, `!=`, `null`, `!null`)
- `lastViewedOperatorSetup()`: Added `"!<>"`
- `firstEncounteredOperatorSetup()`: Added `"!<>"`
- `lastEncounteredOperatorSetup()`: Added `"!<>"`
- `firstViewedOperatorSetup()`: Added `"!<>"`
- `articleCreatedOperatorSetup()`: Added `"!<>"`
- `matchDateOperator()`: Added `"!<>"` case that returns `ts !in from..to`
- Count filter `when` blocks: Added `"!<>"` case for both read and viewed counts
- All validation checks updated to handle `!<>` alongside `<>`
- "To" field clearing logic updated to preserve for `!<>`

---

## Issue 3: Added Sorting by Modified Timestamp, Viewed Count, Encountered Count

**Before:** Sort options were: Last Viewed, Last Encountered, Created, Para Created.

**After:** Sort options are now: Last Viewed, Last Encountered, Created, **Modified**, Para Created, **Viewed Count**, **Encountered Count**.

**Changes in `WIPFilterFragment.kt`:**

- `sortSpinnerSetup()`: Added "Modified", "Viewed Count", "Encountered Count" to sort options list
- `sortFilteredList()`: Added sorting logic:
  - "Modified" sorts by `modifiedAt` timestamp
  - "Viewed Count" sorts by `displayCount` (with null coerced to 0)
  - "Encountered Count" sorts by `readCount` (with null coerced to 0)

---

## Issue 4: Pause Button and ASC/DESC Button Visibility

**Before:**

- Pause button in carousel used `selectableItemBackgroundBorderless` (transparent background), barely visible
- ASC/DESC button in filter used `OutlinedButton` style, hard to see on gray background

**After:**

- Pause button now uses `edit_text_bg` drawable background with padding, making it clearly visible with a defined border
- ASC/DESC button now uses a solid blue (`#1565C0`) background with white text, highly visible

**Changes in `fragment_carousel.xml`:**

- `btnPauseAutoScroll`: Changed background from `?attr/selectableItemBackgroundBorderless` to `@drawable/edit_text_bg`, increased size to 44dp, added 8dp padding

**Changes in `fragment_w_i_p_filter.xml`:**

- `btnSortOrder`: Removed `OutlinedButton` style, added `android:backgroundTint="#1565C0"` and `android:textColor="@color/white"`

---

## Issue 5: Changed Flashcard Option Labels

**Before:**

- "Update View Counts during flashcard"
- "Update Timestamps during flashcard"

**After:**

- "Update Viewed Counts"
- "Update Viewed Timestamps"

**Changes in `fragment_w_i_p_filter.xml`:**

- `cbUpdateViewCount`: Changed `android:text` from "Update View Counts during flashcard" to "Update Viewed Counts"
- `cbUpdateTimestamps`: Changed `android:text` from "Update Timestamps during flashcard" to "Update Viewed Timestamps"

---

## Issue 6: Bottom Three Buttons Alignment on Filter Page

**Before:** The `ll2` LinearLayout used `android:layout_width="match_parent"` with `tools:layout_editor_absoluteX="16dp"` and no proper ConstraintLayout positioning, causing potential misalignment.

**After:** The `ll2` LinearLayout now uses:

- `android:layout_width="0dp"` (constraint-based width)
- Proper `app:layout_constraintStart_toStartOf="parent"` and `app:layout_constraintEnd_toEndOf="parent"`
- `android:gravity="center"` for centered content
- `android:paddingVertical="4dp"` for consistent spacing
- Removed `tools:layout_editor_absoluteX` attributethere is

**Changes in `fragment_w_i_p_filter.xml`:**

- Updated `ll2` LinearLayout constraints and sizing

---

## Issue 9: +/- Buttons on Edit WIP Page

**Status:** Already existed before this change.

**Before/After:** The +/- buttons for Viewed count (`btnViewedPlus`, `btnViewedMinus`) and Encountered count (`btnEncounterPlus`, `btnEncounterMinus`) were already implemented in `fragment_w_i_p_edit.xml` (lines 281-309, 340-371) with click handlers in `WIPEditFragment.kt` (lines 131-142). No changes were needed.

---

## Build & Test Results

- `assembleDebug`: BUILD SUCCESSFUL
- `testOriginalDebugUnitTest`: BUILD SUCCESSFUL (all tests pass)
- Installed and verified on emulator (Pixel 3, API 35)
- Confirmed UI changes visible: labels, button colors, spinner operators, button alignment
