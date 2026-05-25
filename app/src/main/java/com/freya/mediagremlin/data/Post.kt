package com.freya.mediagremlin.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "posts",
    indices = [Index(value = ["urlNormalized"], unique = true)]
)
data class Post(
    @PrimaryKey val id: String,
    val title: String,
    val source: String,
    val platform: String,
    val previewText: String? = null,
    val previewImageUrl: String? = null,
    val url: String,
    val urlNormalized: String,
    val publishedAt: Long = System.currentTimeMillis(),
    val saved: Boolean = false,
    val read: Boolean = false,
    val isTextOnly: Boolean = false
)