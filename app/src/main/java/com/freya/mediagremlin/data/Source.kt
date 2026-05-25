package com.freya.mediagremlin.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sources")
data class Source(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,       // "RSS" | "REDDIT" | "HN" | "MASTODON"
    val url: String,
    val enabled: Boolean = true
)

