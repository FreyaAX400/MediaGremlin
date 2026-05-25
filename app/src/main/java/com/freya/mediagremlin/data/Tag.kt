package com.freya.mediagremlin.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tags")
data class Tag(
    @PrimaryKey val id: String,
    val name: String,
    val patterns: String,   // JSON array of regex strings, e.g. ["\\bLLM\\b","machine learning"]
    val color: Int
)
