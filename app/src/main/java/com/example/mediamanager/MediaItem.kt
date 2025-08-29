package com.example.mediamanager

import android.net.Uri

enum class MediaType {
    IMAGE,
    VIDEO,
    AUDIO
}

data class MediaItem(
    val uri: Uri,
    val name: String,
    val type: MediaType,
    val dateAdded: Long,
    val size: Long,
    val tags: List<String> = emptyList()
)
