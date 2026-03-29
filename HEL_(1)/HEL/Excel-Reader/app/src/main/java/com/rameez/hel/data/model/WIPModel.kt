package com.rameez.hel.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "WIP_LIST")
data class WIPModel constructor(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,

    var sr: Float? = null,
    var category: String? = null,
    var wip: String? = null,
    var meaning: String? = null,
    var sampleSentence: String? = null,
    var customTag: List<String> = emptyList(),

    var readCount: Float? = null,
    var displayCount: Float? = null,
    var createdAt: Long = 0L,
    var modifiedAt: Long = 0L,
    var displayCountUpdatedAt: Long = 0L,
    var readCountUpdatedAt: Long = 0L,
    var firstViewedAt: Long = 0L,
    var firstEncounteredAt: Long = 0L,
    var lastParaCreatedAt: Long = 0L

) {
    data class Builder(
        var id: Int? = null,
        var sr: Float? = null,
        var category: String? = null,
        var wip: String? = null,
        var meaning: String? = null,
        var sampleSentence: String? = null,
        var customTag: List<String> = emptyList(),

        var readCount: Float? = null,
        var displayCount: Float? = null,
        var createdAt: Long = 0L,
        var modifiedAt: Long = 0L,
        var displayCountUpdatedAt: Long = 0L,
        var readCountUpdatedAt: Long = 0L,
        var firstViewedAt: Long = 0L,
        var firstEncounteredAt: Long = 0L,
        var lastParaCreatedAt: Long = 0L

    ) {
        fun id(id: Int?) = apply { this.id = id }
        fun sr(sr: Float?) = apply { this.sr = sr }
        fun category(category: String?) = apply { this.category = category }
        fun wip(wip: String?) = apply { this.wip = wip }
        fun meaning(meaning: String?) = apply { this.meaning = meaning }
        fun sampleSentence(sampleSentence: String?) = apply { this.sampleSentence = sampleSentence }
        fun customTag(customTag: List<String>) = apply { this.customTag = customTag }
        fun readCount(readCount: Float) = apply { this.readCount = readCount }
        fun displayCount(displayCount: Float) = apply { this.displayCount = displayCount }
        fun createdAt(createdAt: Long) = apply { this.createdAt = createdAt }
        fun modifiedAt(modifiedAt: Long) = apply { this.modifiedAt = modifiedAt }
        fun displayCountUpdatedAt(ts: Long) = apply { this.displayCountUpdatedAt = ts }
        fun readCountUpdatedAt(ts: Long) = apply { this.readCountUpdatedAt = ts }
        fun firstViewedAt(ts: Long) = apply { this.firstViewedAt = ts }
        fun firstEncounteredAt(ts: Long) = apply { this.firstEncounteredAt = ts }
        fun lastParaCreatedAt(ts: Long) = apply { this.lastParaCreatedAt = ts }
        fun build() = WIPModel(
            id,
            sr,
            category,
            wip,
            meaning,
            sampleSentence,
            customTag,
            readCount,
            displayCount,
            createdAt,
            modifiedAt,
            displayCountUpdatedAt,
            readCountUpdatedAt,
            firstViewedAt,
            firstEncounteredAt,
            lastParaCreatedAt
        )
    }
}
