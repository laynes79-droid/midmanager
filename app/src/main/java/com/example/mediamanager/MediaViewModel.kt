package com.example.mediamanager

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MediaViewModel : ViewModel() {

    private val _imageItems = mutableStateOf<List<MediaItem>>(emptyList())
    val imageItems: State<List<MediaItem>> = _imageItems

    private val _videoItems = mutableStateOf<List<MediaItem>>(emptyList())
    val videoItems: State<List<MediaItem>> = _videoItems

    private val _audioItems = mutableStateOf<List<MediaItem>>(emptyList())
    val audioItems: State<List<MediaItem>> = _audioItems

    fun loadMedia(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _imageItems.value = queryMediaStore(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, MediaType.IMAGE)
            _videoItems.value = queryMediaStore(context, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, MediaType.VIDEO)
            _audioItems.value = queryMediaStore(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, MediaType.AUDIO)
        }
    }

    private fun queryMediaStore(context: Context, uri: Uri, type: MediaType): List<MediaItem> {
        val items = mutableListOf<MediaItem>()

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME
        )

        context.contentResolver.query(
            uri,
            projection,
            null,
            null,
            "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val contentUri = ContentUris.withAppendedId(uri, id)

                items.add(MediaItem(uri = contentUri, name = name, type = type))
            }
        }
        return items
    }
}
