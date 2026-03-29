package com.rameez.hel.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "GENERATED_ARTICLES")
data class ArticleModel(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val content: String,
    val wipIds: List<Int> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)
