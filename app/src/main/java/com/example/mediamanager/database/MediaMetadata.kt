package com.example.mediamanager.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity(tableName = "media_metadata")
@TypeConverters(Converters::class)
data class MediaMetadata(
    @PrimaryKey val uri: String,
    val tags: List<String>
)
