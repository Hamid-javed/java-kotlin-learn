package com.rameez.hel.data.repository

import android.util.Log
import androidx.lifecycle.LiveData
import com.rameez.hel.data.WIPDao
import com.rameez.hel.data.model.WIPModel
import com.rameez.hel.data.model.ArticleModel


class WIPRepository(private val wipDao: WIPDao) {


    suspend fun insertWIP(wipItem: WIPModel) {
        val now = System.currentTimeMillis()

        if (wipItem.createdAt == 0L) {
            wipItem.createdAt = now
        }

        // Fix: Set timestamps when counts are non-zero on creation
        val rc = wipItem.readCount ?: 0f
        if (rc > 0f) {
            if (wipItem.readCountUpdatedAt == 0L) wipItem.readCountUpdatedAt = now
            if (wipItem.firstEncounteredAt == 0L) wipItem.firstEncounteredAt = now
        }

        val vc = wipItem.displayCount ?: 0f
        if (vc > 0f) {
            if (wipItem.displayCountUpdatedAt == 0L) wipItem.displayCountUpdatedAt = now
            if (wipItem.firstViewedAt == 0L) wipItem.firstViewedAt = now
        }

        wipDao.insertWIP(wipItem)
    }


    fun getWIPs(): LiveData<List<WIPModel>> = wipDao.getWIPs()

    suspend fun getWIPs2(): List<WIPModel> = wipDao.getWIPs2()

    suspend fun dropTable() {
        wipDao.dropTable()
    }

    fun getWIPById(id: Int): LiveData<WIPModel> = wipDao.getWIPById(id)

    suspend fun getWIPByIdSync(id: Int) = wipDao.getWIPByIdSync(id)


    suspend fun updateWIP(
        id: Int,
        category: String,
        wip: String,
        meaning: String,
        sampleSentence: String,
        customTag: List<String>,
        readCount: Float,
        viewedCount: Float
    ) {
        val now = System.currentTimeMillis()
        val prev = getWIPByIdSync(id)



        val prevTags = prev?.customTag ?: emptyList()
        val newTags = customTag

        val contentChanged =
            prev?.wip != wip ||
                    prev.meaning != meaning ||
                    prev.sampleSentence != sampleSentence ||
                    prev.category != category ||
                    prevTags != newTags


        val newModifiedAt = if (contentChanged) now else (prev?.modifiedAt ?: 0L)

        // The timestamp fields below are managed by dedicated increment/reset functions
        // and should not be modified by a general WIP content update.
        wipDao.updateWIP(
            id,
            category,
            wip,
            meaning,
            sampleSentence,
            customTag,
            readCount,
            viewedCount,
            modifiedAt = newModifiedAt,
            lastParaCreatedAt = prev?.lastParaCreatedAt ?: 0L
        )
    }

    suspend fun updateReadCount(id: Int, readCount: Float) {
        val prev = wipDao.getWIPByIdSync(id) ?: return
        val now = System.currentTimeMillis()

        val prevReadCount = prev.readCount ?: 0f

        when {
            // Manual reset to zero - also resets Last Encountered timestamp
            readCount == 0f -> {
                wipDao.updateReadCountWithTs(id, 0f, 0L)
            }

            // Increment - updates Last Encountered timestamp
            readCount > prevReadCount -> {
                wipDao.updateReadCountWithTs(id, readCount, now)

                // Set First Encountered if it hasn't been set yet
                if (prev.firstEncounteredAt == 0L) {
                    wipDao.updateFirstEncounteredAt(id, now)
                }
            }

            // Decrement (do NOT touch timestamp)
            else -> {
                // Retain previous timestamp for decrements
                wipDao.updateReadCountWithTs(
                    id,
                    readCount,
                    prev.readCountUpdatedAt ?: 0L
                )
            }
        }
    }



    suspend fun updateViewedCount(id: Int, viewCount: Float) {
        val prev = wipDao.getWIPByIdSync(id) ?: return
        val now = System.currentTimeMillis()

        // Unwrap nullable DB values ONCE
        val prevViewedCount = prev.displayCount ?: 0f

        when {
            // Manual reset to zero - also resets Last Viewed timestamp
            viewCount == 0f -> {
                wipDao.updateViewedCountWithTs(id, 0f, 0L)
            }

            // Increment - updates Last Viewed timestamp
            viewCount > prevViewedCount -> {
                wipDao.updateViewedCountWithTs(id, viewCount, now)

                // Set First Viewed if it hasn't been set yet
                if (prev.firstViewedAt == 0L) {
                    wipDao.updateFirstViewedAt(id, now)
                }
            }

            // Decrement (do NOT touch timestamp)
            else -> {
                // Retain previous timestamp for decrements
                wipDao.updateViewedCountWithTs(
                    id,
                    viewCount,
                    prev.displayCountUpdatedAt ?: 0L
                )
            }
        }
    }



    suspend fun deleteWIPById(id: Int) {
        wipDao.deleteWIPbyId(id)
    }

    suspend fun deleteWholeCategory(categories: List<String?>) {
        wipDao.deleteWholeCategory(categories)
    }


    suspend fun updateTagsOnly(id: Int?, tags: List<String>) {
        wipDao.updateTagsOnly(id, tags)
    }


    suspend fun resetEncountered(id: Int) {
        wipDao.resetEncountered(id)
    }


    suspend fun resetViewed(id: Int) {
        wipDao.resetViewed(id)
    }


    suspend fun resetEncounteredForCategories(categories: List<String>) {
        wipDao.resetEncounteredForCategories(categories)
    }

    suspend fun resetViewedForCategories(categories: List<String>) {
        wipDao.resetViewedForCategories(categories)
    }



    suspend fun getWIP2ById(id: Int): WIPModel? {
        return wipDao.getWIPByIdSync(id)
    }



    suspend fun incrementDisplayCount(wipId: Int, inc: Float = 1f, updateTimestamp: Boolean = true) {
        val now = System.currentTimeMillis()
        val currentWip = wipDao.getWIPByIdSync(wipId) ?: return

        if (updateTimestamp) {
            wipDao.incrementDisplayCountSql(wipId, inc, now)

            if (currentWip.firstViewedAt == 0L) {
                wipDao.updateFirstViewedAt(wipId, now)
            }
        } else {
            wipDao.incrementDisplayCountOnly(wipId, inc)
        }
    }

    suspend fun updateLastViewedTimestamp(wipId: Int) {
        val now = System.currentTimeMillis()
        val currentWip = wipDao.getWIPByIdSync(wipId) ?: return

        wipDao.updateDisplayCountTimestampOnly(wipId, now)

        if (currentWip.firstViewedAt == 0L) {
            wipDao.updateFirstViewedAt(wipId, now)
        }
    }

    suspend fun insertArticle(article: ArticleModel) {
        wipDao.insertArticle(article)
    }

    fun getLatestArticle(): LiveData<ArticleModel?> {
        return wipDao.getLatestArticle()
    }

    suspend fun getAllArticles(): List<ArticleModel> {
        return wipDao.getAllArticles()
    }

    suspend fun updateLastParaCreatedAt(id: Int, ts: Long) {
        wipDao.updateLastParaCreatedAt(id, ts)
    }

    suspend fun resetParaCreatedAt(id: Int) {
        wipDao.resetParaCreatedAt(id)
    }



}
