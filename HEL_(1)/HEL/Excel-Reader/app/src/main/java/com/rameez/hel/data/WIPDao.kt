package com.rameez.hel.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.rameez.hel.data.model.WIPModel
import com.rameez.hel.data.model.ArticleModel


@Dao
interface WIPDao {

    @Insert
    suspend fun insertWIP(wipItem: WIPModel)

    @Query("SELECT * FROM WIP_LIST")
    fun getWIPs(): LiveData<List<WIPModel>>

    @Query("SELECT * FROM WIP_LIST")
    suspend fun getWIPs2(): List<WIPModel>

    @Query("DELETE FROM WIP_LIST")
    suspend fun dropTable()

    @Query("SELECT * FROM WIP_LIST WHERE id= :id")
    fun getWIPById(id: Int): LiveData<WIPModel>

    // Synchronous suspend getter — used by repository to compare previous values
    @Query("SELECT * FROM WIP_LIST WHERE id = :id LIMIT 1")
    suspend fun getWIPByIdSync(id: Int): WIPModel?

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
    suspend fun updateWIP(
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
    suspend fun updateReadCountWithTs(id: Int, readCount: Float, ts: Long)

    // Update only displayCount with timestamp (set exact value)
    @Query("""
        UPDATE WIP_LIST SET
            displayCount = :viewCount,
            displayCountUpdatedAt = :ts
        WHERE id = :id
    """)
    suspend fun updateViewedCountWithTs(id: Int, viewCount: Float, ts: Long)

    // Atomic increment of displayCount (safer for concurrent increments)
    @Query("""
        UPDATE WIP_LIST SET
            displayCount = COALESCE(displayCount, 0) + :inc,
            displayCountUpdatedAt = :ts
        WHERE id = :id
    """)
    suspend fun incrementDisplayCountSql(id: Int, inc: Float, ts: Long)

    // Increment displayCount WITHOUT updating timestamp
    @Query("""
        UPDATE WIP_LIST SET
            displayCount = COALESCE(displayCount, 0) + :inc
        WHERE id = :id
    """)
    suspend fun incrementDisplayCountOnly(id: Int, inc: Float)

    // Update displayCountUpdatedAt (last viewed timestamp) WITHOUT incrementing count
    @Query("""
        UPDATE WIP_LIST SET
            displayCountUpdatedAt = :ts
        WHERE id = :id
    """)
    suspend fun updateDisplayCountTimestampOnly(id: Int, ts: Long)

    @Query("SELECT * FROM WIP_LIST WHERE customTag LIKE '%' || :tag || '%'")
    fun getWIPsWithCustomTag(tag: String): LiveData<List<WIPModel>>

    @Query("DELETE FROM WIP_LIST WHERE id = :id")
    suspend fun deleteWIPbyId(id: Int)

    @Query("DELETE FROM WIP_LIST WHERE category  IN (:categories)")
    suspend fun deleteWholeCategory(categories: List<String?>)


    @Query("""
UPDATE WIP_LIST SET
    readCount = 0.0,
    readCountUpdatedAt = 0
WHERE id = :id
""")
    suspend fun resetEncountered(id: Int)


    @Query("""
UPDATE WIP_LIST SET
    displayCount = 0.0,
    displayCountUpdatedAt = 0
WHERE id = :id
""")
    suspend fun resetViewed(id: Int)


    @Query("""
UPDATE WIP_LIST SET
    readCount = 0.0,
    readCountUpdatedAt = 0
WHERE LOWER(category) IN (:categories)
""")
    suspend fun resetEncounteredForCategories(categories: List<String>)



    @Query("""
UPDATE WIP_LIST SET
    displayCount = 0.0,
    displayCountUpdatedAt = 0
WHERE LOWER(category) IN (:categories)
""")
    suspend fun resetViewedForCategories(categories: List<String>)




    @Query("""
    UPDATE WIP_LIST SET
        customTag = :tags
    WHERE id = :id
""")
    suspend fun updateTagsOnly(id: Int?, tags: List<String>)

    @Query("UPDATE WIP_LIST SET firstViewedAt = :ts WHERE id = :id")
    suspend fun updateFirstViewedAt(id: Int, ts: Long)

    @Query("UPDATE WIP_LIST SET firstEncounteredAt = :ts WHERE id = :id")
    suspend fun updateFirstEncounteredAt(id: Int, ts: Long)

    @Query("SELECT * FROM WIP_LIST WHERE id = :id LIMIT 1")
    fun getWIPByIdLive(id: Int): LiveData<WIPModel>

    @Query("UPDATE WIP_LIST SET lastParaCreatedAt = :ts WHERE id = :id")
    suspend fun updateLastParaCreatedAt(id: Int, ts: Long)

    @Query("UPDATE WIP_LIST SET lastParaCreatedAt = 0 WHERE id = :id")
    suspend fun resetParaCreatedAt(id: Int)

    // Article methods
    @Insert
    suspend fun insertArticle(article: ArticleModel)

    @Query("SELECT * FROM GENERATED_ARTICLES ORDER BY createdAt DESC LIMIT 1")
    fun getLatestArticle(): LiveData<ArticleModel?>

    @Query("SELECT * FROM GENERATED_ARTICLES")
    suspend fun getAllArticles(): List<ArticleModel>

    // === BULK ACTIONS ===

    @Query("SELECT * FROM WIP_LIST WHERE id IN (:ids)")
    suspend fun getWIPsByIds(ids: List<Int>): List<WIPModel>

    @Query("UPDATE WIP_LIST SET readCount = 0, readCountUpdatedAt = 0, firstEncounteredAt = 0 WHERE id IN (:ids)")
    suspend fun bulkResetEncounteredCount(ids: List<Int>)

    @Query("UPDATE WIP_LIST SET displayCount = 0, displayCountUpdatedAt = 0, firstViewedAt = 0 WHERE id IN (:ids)")
    suspend fun bulkResetViewedCount(ids: List<Int>)

    @Query("UPDATE WIP_LIST SET customTag = '' WHERE id IN (:ids)")
    suspend fun bulkRemoveAllTags(ids: List<Int>)

    @Query("UPDATE WIP_LIST SET createdAt = 0 WHERE id IN (:ids)")
    suspend fun bulkResetCreatedAt(ids: List<Int>)

    @Query("UPDATE WIP_LIST SET modifiedAt = 0 WHERE id IN (:ids)")
    suspend fun bulkResetModifiedAt(ids: List<Int>)

    @Query("UPDATE WIP_LIST SET firstViewedAt = 0 WHERE id IN (:ids)")
    suspend fun bulkResetFirstViewedAt(ids: List<Int>)

    @Query("UPDATE WIP_LIST SET firstEncounteredAt = 0 WHERE id IN (:ids)")
    suspend fun bulkResetFirstEncounteredAt(ids: List<Int>)

    @Query("UPDATE WIP_LIST SET displayCountUpdatedAt = 0 WHERE id IN (:ids)")
    suspend fun bulkResetLastViewedAt(ids: List<Int>)

    @Query("UPDATE WIP_LIST SET readCountUpdatedAt = 0 WHERE id IN (:ids)")
    suspend fun bulkResetLastEncounteredAt(ids: List<Int>)

    @Query("UPDATE WIP_LIST SET lastParaCreatedAt = 0 WHERE id IN (:ids)")
    suspend fun bulkResetLastParaCreatedAt(ids: List<Int>)

    @Query("UPDATE WIP_LIST SET customTag = :tags WHERE id = :id")
    suspend fun updateTagsById(id: Int, tags: String)

}
