package com.rameez.hel.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.rameez.hel.data.model.WIPModel
import com.rameez.hel.data.model.ArticleModel


@Dao
abstract class WIPDao {

    @Insert
    abstract suspend fun insertWIP(wipItem: WIPModel)

    @Query("SELECT * FROM WIP_LIST")
    abstract fun getWIPs(): LiveData<List<WIPModel>>

    @Query("SELECT * FROM WIP_LIST")
    abstract suspend fun getWIPs2(): List<WIPModel>

    @Query("DELETE FROM WIP_LIST")
    abstract suspend fun dropTable()

    @Query("SELECT * FROM WIP_LIST WHERE id= :id")
    abstract fun getWIPById(id: Int): LiveData<WIPModel>

    // Synchronous suspend getter — used by repository to compare previous values
    @Query("SELECT * FROM WIP_LIST WHERE id = :id LIMIT 1")
    abstract suspend fun getWIPByIdSync(id: Int): WIPModel?

    // Update full WIP content plus modifiedAt and readCount, viewedCount values.
    // Timestamps for read/view counts and first viewed/encountered are handled separately.
    @Query("""
    UPDATE WIP_LIST SET
        category = :category,
        wip = :wip,
        meaning = :meaning,
        sampleSentence = :sampleSentence,
        customTag = :customTag,
        readCount = :readCount,
        displayCount = :viewedCount,
        modifiedAt = :modifiedAt,
        lastParaCreatedAt = :lastParaCreatedAt
    WHERE id = :id
""")
    abstract suspend fun updateWIP(
        id: Int,
        category: String,
        wip: String,
        meaning: String,
        sampleSentence: String,
        customTag: List<String>,
        readCount: Float,
        viewedCount: Float,
        modifiedAt: Long,
        lastParaCreatedAt: Long
    )

    // Update only readCount with timestamp
    @Query("""
        UPDATE WIP_LIST SET
            readCount = :readCount,
            readCountUpdatedAt = :ts
        WHERE id = :id
    """)
    abstract suspend fun updateReadCountWithTs(id: Int, readCount: Float, ts: Long)

    // Update only displayCount with timestamp (set exact value)
    @Query("""
        UPDATE WIP_LIST SET
            displayCount = :viewCount,
            displayCountUpdatedAt = :ts
        WHERE id = :id
    """)
    abstract suspend fun updateViewedCountWithTs(id: Int, viewCount: Float, ts: Long)

    // Atomic increment of displayCount (safer for concurrent increments)
    @Query("""
        UPDATE WIP_LIST SET
            displayCount = COALESCE(displayCount, 0) + :inc,
            displayCountUpdatedAt = :ts
        WHERE id = :id
    """)
    abstract suspend fun incrementDisplayCountSql(id: Int, inc: Float, ts: Long)

    // Increment displayCount WITHOUT updating timestamp
    @Query("""
        UPDATE WIP_LIST SET
            displayCount = COALESCE(displayCount, 0) + :inc
        WHERE id = :id
    """)
    abstract suspend fun incrementDisplayCountOnly(id: Int, inc: Float)

    // Update displayCountUpdatedAt (last viewed timestamp) WITHOUT incrementing count
    @Query("""
        UPDATE WIP_LIST SET
            displayCountUpdatedAt = :ts
        WHERE id = :id
    """)
    abstract suspend fun updateDisplayCountTimestampOnly(id: Int, ts: Long)

    @Query("SELECT * FROM WIP_LIST WHERE customTag LIKE '%' || :tag || '%'")
    abstract fun getWIPsWithCustomTag(tag: String): LiveData<List<WIPModel>>

    @Query("DELETE FROM WIP_LIST WHERE id = :id")
    abstract suspend fun deleteWIPbyId(id: Int)

    @Query("DELETE FROM WIP_LIST WHERE category  IN (:categories)")
    abstract suspend fun deleteWholeCategory(categories: List<String?>)


    @Query("""
UPDATE WIP_LIST SET
    readCount = 0.0,
    readCountUpdatedAt = 0
WHERE id = :id
""")
    abstract suspend fun resetEncountered(id: Int)


    @Query("""
UPDATE WIP_LIST SET
    displayCount = 0.0,
    displayCountUpdatedAt = 0
WHERE id = :id
""")
    abstract suspend fun resetViewed(id: Int)


    @Query("""
UPDATE WIP_LIST SET
    readCount = 0.0,
    readCountUpdatedAt = 0
WHERE LOWER(category) IN (:categories)
""")
    abstract suspend fun resetEncounteredForCategories(categories: List<String>)



    @Query("""
UPDATE WIP_LIST SET
    displayCount = 0.0,
    displayCountUpdatedAt = 0
WHERE LOWER(category) IN (:categories)
""")
    abstract suspend fun resetViewedForCategories(categories: List<String>)




    @Query("""
    UPDATE WIP_LIST SET
        customTag = :tags
    WHERE id = :id
""")
    abstract suspend fun updateTagsOnly(id: Int?, tags: List<String>)

    @Query("UPDATE WIP_LIST SET firstViewedAt = :ts WHERE id = :id")
    abstract suspend fun updateFirstViewedAt(id: Int, ts: Long)

    @Query("UPDATE WIP_LIST SET firstEncounteredAt = :ts WHERE id = :id")
    abstract suspend fun updateFirstEncounteredAt(id: Int, ts: Long)

    @Query("SELECT * FROM WIP_LIST WHERE id = :id LIMIT 1")
    abstract fun getWIPByIdLive(id: Int): LiveData<WIPModel>

    @Query("UPDATE WIP_LIST SET lastParaCreatedAt = :ts WHERE id = :id")
    abstract suspend fun updateLastParaCreatedAt(id: Int, ts: Long)

    @Query("UPDATE WIP_LIST SET lastParaCreatedAt = 0 WHERE id = :id")
    abstract suspend fun resetParaCreatedAt(id: Int)

    // Article methods
    @Insert
    abstract suspend fun insertArticle(article: ArticleModel)

    @Query("SELECT * FROM GENERATED_ARTICLES ORDER BY createdAt DESC LIMIT 1")
    abstract fun getLatestArticle(): LiveData<ArticleModel?>

    @Query("SELECT * FROM GENERATED_ARTICLES")
    abstract suspend fun getAllArticles(): List<ArticleModel>

    // === BULK ACTIONS ===

    @Query("SELECT * FROM WIP_LIST WHERE id IN (:ids)")
    abstract suspend fun getWIPsByIds(ids: List<Int>): List<WIPModel>

    @Query("UPDATE WIP_LIST SET readCount = 0, readCountUpdatedAt = 0 WHERE id IN (:ids)")
    abstract suspend fun bulkResetEncounteredCount(ids: List<Int>)

    @Query("UPDATE WIP_LIST SET displayCount = 0, displayCountUpdatedAt = 0 WHERE id IN (:ids)")
    abstract suspend fun bulkResetViewedCount(ids: List<Int>)

    @Query("UPDATE WIP_LIST SET customTag = '' WHERE id IN (:ids)")
    abstract suspend fun bulkRemoveAllTags(ids: List<Int>)

    @Query("UPDATE WIP_LIST SET createdAt = 0 WHERE id IN (:ids)")
    abstract suspend fun bulkResetCreatedAt(ids: List<Int>)

    @Query("UPDATE WIP_LIST SET modifiedAt = 0 WHERE id IN (:ids)")
    abstract suspend fun bulkResetModifiedAt(ids: List<Int>)

    @Query("UPDATE WIP_LIST SET firstViewedAt = 0 WHERE id IN (:ids)")
    abstract suspend fun bulkResetFirstViewedAt(ids: List<Int>)

    @Query("UPDATE WIP_LIST SET firstEncounteredAt = 0 WHERE id IN (:ids)")
    abstract suspend fun bulkResetFirstEncounteredAt(ids: List<Int>)

    @Query("UPDATE WIP_LIST SET displayCountUpdatedAt = 0 WHERE id IN (:ids)")
    abstract suspend fun bulkResetLastViewedAt(ids: List<Int>)

    @Query("UPDATE WIP_LIST SET readCountUpdatedAt = 0 WHERE id IN (:ids)")
    abstract suspend fun bulkResetLastEncounteredAt(ids: List<Int>)

    @Query("UPDATE WIP_LIST SET lastParaCreatedAt = 0 WHERE id IN (:ids)")
    abstract suspend fun bulkResetLastParaCreatedAt(ids: List<Int>)

    @Query("UPDATE WIP_LIST SET customTag = :tags WHERE id = :id")
    abstract suspend fun updateTagsById(id: Int, tags: List<String>)

    @Transaction
    open suspend fun executeBulkActions(
        ids: List<Int>,
        resetEncountered: Boolean,
        resetViewed: Boolean,
        removeAllTags: Boolean,
        addTag: String?,
        resetCreatedAt: Boolean,
        resetModifiedAt: Boolean,
        resetFirstViewedAt: Boolean,
        resetFirstEncounteredAt: Boolean,
        resetLastViewedAt: Boolean,
        resetLastEncounteredAt: Boolean,
        resetLastParaCreatedAt: Boolean
    ) {
        if (ids.isEmpty()) return

        if (resetEncountered) bulkResetEncounteredCount(ids)
        if (resetViewed) bulkResetViewedCount(ids)

        if (removeAllTags) bulkRemoveAllTags(ids)

        if (!addTag.isNullOrBlank()) {
            val wips = getWIPsByIds(ids)
            val trimmedTag = addTag.trim()
            for (wip in wips) {
                val currentTags = wip.customTag.toMutableList()
                if (!currentTags.contains(trimmedTag)) {
                    currentTags.add(trimmedTag)
                    wip.id?.let { updateTagsById(it, currentTags) }
                }
            }
        }

        if (resetCreatedAt) bulkResetCreatedAt(ids)
        if (resetModifiedAt) bulkResetModifiedAt(ids)
        if (resetFirstViewedAt) bulkResetFirstViewedAt(ids)
        if (resetFirstEncounteredAt) bulkResetFirstEncounteredAt(ids)
        if (resetLastViewedAt) bulkResetLastViewedAt(ids)
        if (resetLastEncounteredAt) bulkResetLastEncounteredAt(ids)
        if (resetLastParaCreatedAt) bulkResetLastParaCreatedAt(ids)
    }

}
