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
    val type: MediaType
)
