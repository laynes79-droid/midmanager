package com.example.mediamanager

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

enum class SortOrder {
    BY_DATE_DESC,
    BY_NAME_ASC,
    BY_SIZE_DESC
}

class MediaViewModel : ViewModel() {

    private val _allMediaItems = MutableStateFlow<List<MediaItem>>(emptyList())

    val searchQuery = MutableStateFlow("")
    val sortOrder = MutableStateFlow(SortOrder.BY_DATE_DESC)

    val imageItems = searchQuery.combine(sortOrder) { query, order ->
        filterAndSort(_allMediaItems.value, MediaType.IMAGE, query, order)
    }
    val videoItems = searchQuery.combine(sortOrder) { query, order ->
        filterAndSort(_allMediaItems.value, MediaType.VIDEO, query, order)
    }
    val audioItems = searchQuery.combine(sortOrder) { query, order ->
        filterAndSort(_allMediaItems.value, MediaType.AUDIO, query, order)
    }

    fun onSearchQueryChanged(newQuery: String) {
        searchQuery.value = newQuery
    }

    fun onSortOrderChanged(newSortOrder: SortOrder) {
        sortOrder.value = newSortOrder
    }

    private fun filterAndSort(
        items: List<MediaItem>,
        type: MediaType,
        query: String,
        order: SortOrder
    ): List<MediaItem> {
        val filtered = items.filter {
            it.type == type && it.name.contains(query, ignoreCase = true)
        }
        return when (order) {
            SortOrder.BY_DATE_DESC -> filtered.sortedByDescending { it.dateAdded }
            SortOrder.BY_NAME_ASC -> filtered.sortedBy { it.name }
            SortOrder.BY_SIZE_DESC -> filtered.sortedByDescending { it.size }
        }
    }

    fun loadMedia(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val allItems = mutableListOf<MediaItem>()

            allItems.addAll(queryMediaStore(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, MediaType.IMAGE))
            allItems.addAll(queryMediaStore(context, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, MediaType.VIDEO))
            allItems.addAll(queryMediaStore(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, MediaType.AUDIO))

            _allMediaItems.value = allItems
        }
    }

    private fun queryMediaStore(context: Context, uri: Uri, type: MediaType): List<MediaItem> {
        val items = mutableListOf<MediaItem>()

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.SIZE
        )

        context.contentResolver.query(
            uri,
            projection,
            null,
            null,
            null
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val date = cursor.getLong(dateColumn)
                val size = cursor.getLong(sizeColumn)
                val contentUri = ContentUris.withAppendedId(uri, id)

                items.add(MediaItem(uri = contentUri, name = name, type = type, dateAdded = date, size = size))
            }
        }
        return items
    }
}
