package com.rameez.hel.viewmodel

import androidx.lifecycle.ViewModel
import com.rameez.hel.data.model.WIPModel

class SharedViewModel : ViewModel() {

    var isMuted: Boolean = false
    var isFilterApplied: Boolean = false
    var isShowingFilteredResults: Boolean = false
    var filteredWipsList = mutableListOf<WIPModel>()
    var selectedHours: Int? = null
    var selectedMins: Int? = null
    var selectedSecs: Int?= null
    var tagsList = mutableListOf<String>()
    var categoryList = mutableListOf<String>()
    var readCount: Float? = null
    var viewedCount: Float? = null
    var filteredWord: String? = null
    var filteredMeaning: String? = null
    var filteredSampleSen: String? = null
    var isTimerRunning: Boolean = false
    var readOperator: String? = null
    var viewedOperator: String? = null


    var createdAt: Long? = null
    var createdAtTo: Long? = null
    var modifiedAt: Long? = null
    var modifiedAtTo: Long? = null
    var createdOperator: String? = null
    var modifiedOperator: String? = null
    val leftSwipedItemList = arrayListOf<WIPModel>()
    var notDeletedTags: ArrayList<String>? = null
    var itemPos: Int? = null
    var itemId: Int? = null
    var itemPosFromHome: Int? = null
    var itemIdFromHome: Int? = null
    var isReadAloud: Boolean = false
    var isWIPDeleted = false
    var isWipAdded: Boolean = false

    var isAutoScrollEnabled: Boolean = false
    var autoScrollIntervalSecs: Int = 5
    var isAutoScrollPaused: Boolean = false

    var ttsOptions: MutableList<String> = mutableListOf()

    var remainingTimeInMillis: Long? = null

    // Carousel controls
    var updateViewCountDuringFlashcard: Boolean = true
    var updateTimestampsDuringFlashcard: Boolean = true

    // Sort option for filter results
    var sortBy: String? = null  // "Last Viewed", "Last Encountered", "Created", "Para Created"
    var sortAscending: Boolean = false  // false = descending (newest first), true = ascending (oldest first)


    var lastViewedAt: Long? = null
    var lastViewedAtTo: Long? = null
    var lastViewedOperator: String? = null

    // First Encountered At ---
    var firstEncounteredAt: Long? = null
    var firstEncounteredAtTo: Long? = null
    var firstEncounteredOperator: String? = null

    // Last Encountered At ---
    var lastEncounteredAt: Long? = null
    var lastEncounteredAtTo: Long? = null
    var lastEncounteredOperator: String? = null

    // First Viewed At ---
    var firstViewedAt: Long? = null
    var firstViewedAtTo: Long? = null
    var firstViewedOperator: String? = null

    var selectedTags = mutableSetOf<String>()
    var selectedCategories = mutableSetOf<String>()

    // Article (Para) Filter ---
    var articleCreatedAt: Long? = null
    var articleCreatedAtTo: Long? = null
    var articleCreatedOperator: String? = null

}
