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

    @Query("UPDATE WIP_LIST SET readCount = :value, readCountUpdatedAt = :ts WHERE id IN (:ids)")
    abstract suspend fun bulkSetEncountered(ids: List<Int>, value: Float, ts: Long)

    @Query("UPDATE WIP_LIST SET displayCount = :value, displayCountUpdatedAt = :ts WHERE id IN (:ids)")
    abstract suspend fun bulkSetViewed(ids: List<Int>, value: Float, ts: Long)

    @Query("UPDATE WIP_LIST SET customTag = '' WHERE id IN (:ids)")
    abstract suspend fun bulkRemoveAllTags(ids: List<Int>)

    @Query("UPDATE WIP_LIST SET createdAt = :ts WHERE id IN (:ids)")
    abstract suspend fun bulkSetCreatedAt(ids: List<Int>, ts: Long)

    @Query("UPDATE WIP_LIST SET modifiedAt = :ts WHERE id IN (:ids)")
    abstract suspend fun bulkSetModifiedAt(ids: List<Int>, ts: Long)

    @Query("UPDATE WIP_LIST SET firstViewedAt = :ts WHERE id IN (:ids)")
    abstract suspend fun bulkSetFirstViewedAt(ids: List<Int>, ts: Long)

    @Query("UPDATE WIP_LIST SET firstEncounteredAt = :ts WHERE id IN (:ids)")
    abstract suspend fun bulkSetFirstEncounteredAt(ids: List<Int>, ts: Long)

    @Query("UPDATE WIP_LIST SET displayCountUpdatedAt = :ts WHERE id IN (:ids)")
    abstract suspend fun bulkSetLastViewedAt(ids: List<Int>, ts: Long)

    @Query("UPDATE WIP_LIST SET readCountUpdatedAt = :ts WHERE id IN (:ids)")
    abstract suspend fun bulkSetLastEncounteredAt(ids: List<Int>, ts: Long)

    @Query("UPDATE WIP_LIST SET lastParaCreatedAt = :ts WHERE id IN (:ids)")
    abstract suspend fun bulkSetLastParaCreatedAt(ids: List<Int>, ts: Long)

    @Query("UPDATE WIP_LIST SET customTag = :tags WHERE id = :id")
    abstract suspend fun updateTagsById(id: Int, tags: List<String>)

    /**
     * Apply a batch of bulk edits inside a single transaction.
     *
     * A null for any of the Float?/Long? value arguments means "do not touch this field".
     * A non-null value (including 0/0L) means "set the field to this value for all selected WPIs".
     * Tag add / remove are case-insensitive.
     */
    @Transaction
    open suspend fun executeBulkActions(
        ids: List<Int>,
        setEncountered: Float?,
        setViewed: Float?,
        removeAllTags: Boolean,
        addTag: String?,
        removeTag: String?,
        setCreatedAt: Long?,
        setModifiedAt: Long?,
        setFirstViewedAt: Long?,
        setFirstEncounteredAt: Long?,
        setLastViewedAt: Long?,
        setLastEncounteredAt: Long?,
        setLastParaCreatedAt: Long?
    ) {
        if (ids.isEmpty()) return

        val now = System.currentTimeMillis()

        if (setEncountered != null) {
            // When the count is a positive number, stamp the "last encountered" timestamp to now
            // so the timestamp filters agree with the data. Setting to 0 also resets the timestamp.
            val ts = if (setEncountered > 0f) now else 0L
            bulkSetEncountered(ids, setEncountered, ts)
        }
        if (setViewed != null) {
            val ts = if (setViewed > 0f) now else 0L
            bulkSetViewed(ids, setViewed, ts)
        }

        if (removeAllTags) bulkRemoveAllTags(ids)

        val trimmedAdd = addTag?.trim().orEmpty()
        val trimmedRemove = removeTag?.trim().orEmpty()
        if (trimmedAdd.isNotEmpty() || trimmedRemove.isNotEmpty()) {
            val wips = getWIPsByIds(ids)
            for (wip in wips) {
                val id = wip.id ?: continue
                val currentTags = wip.customTag.toMutableList()
                var changed = false

                if (trimmedRemove.isNotEmpty()) {
                    val sizeBefore = currentTags.size
                    currentTags.removeAll { it.equals(trimmedRemove, ignoreCase = true) }
                    if (currentTags.size != sizeBefore) changed = true
                }

                if (trimmedAdd.isNotEmpty()) {
                    val alreadyHas = currentTags.any { it.equals(trimmedAdd, ignoreCase = true) }
                    if (!alreadyHas) {
                        currentTags.add(trimmedAdd)
                        changed = true
                    }
                }

                if (changed) updateTagsById(id, currentTags)
            }
        }

        if (setCreatedAt != null) bulkSetCreatedAt(ids, setCreatedAt)
        if (setModifiedAt != null) bulkSetModifiedAt(ids, setModifiedAt)
        if (setFirstViewedAt != null) bulkSetFirstViewedAt(ids, setFirstViewedAt)
        if (setFirstEncounteredAt != null) bulkSetFirstEncounteredAt(ids, setFirstEncounteredAt)
        if (setLastViewedAt != null) bulkSetLastViewedAt(ids, setLastViewedAt)
        if (setLastEncounteredAt != null) bulkSetLastEncounteredAt(ids, setLastEncounteredAt)
        if (setLastParaCreatedAt != null) bulkSetLastParaCreatedAt(ids, setLastParaCreatedAt)
    }

}
