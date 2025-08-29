package com.example.mediamanager

import android.app.Application
import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mediamanager.database.AppDatabase
import com.example.mediamanager.database.MediaMetadata
import com.example.mediamanager.database.MediaMetadataDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SortOrder {
    BY_DATE_DESC,
    BY_NAME_ASC,
    BY_SIZE_DESC
}

class MediaViewModel(application: Application) : AndroidViewModel(application) {

    private val mediaMetadataDao: MediaMetadataDao
    private var allMediaItems: List<MediaItem> = emptyList()

    private val _imageItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val imageItems = _imageItems.asStateFlow()

    private val _videoItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val videoItems = _videoItems.asStateFlow()

    private val _audioItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val audioItems = _audioItems.asStateFlow()

    val allTags = MutableStateFlow<List<String>>(emptyList())

    val searchQuery = MutableStateFlow("")
    val sortOrder = MutableStateFlow(SortOrder.BY_DATE_DESC)
    val tagFilter = MutableStateFlow<String?>(null)

    init {
        mediaMetadataDao = AppDatabase.getDatabase(application).mediaMetadataDao()
        loadMedia()

        viewModelScope.launch {
            combine(searchQuery, sortOrder, tagFilter) { query, order, tag ->
                Triple(query, order, tag)
            }.collect { (query, order, tag) ->
                updateFilteredLists(query, order, tag)
            }
        }
    }

    private fun updateFilteredLists(query: String, order: SortOrder, tag: String?) {
        val filtered = allMediaItems.filter { item ->
            val matchesQuery = item.name.contains(query, ignoreCase = true)
            val matchesTag = tag == null || item.tags.contains(tag)
            matchesQuery && matchesTag
        }

        val sorted = when (order) {
            SortOrder.BY_DATE_DESC -> filtered.sortedByDescending { it.dateAdded }
            SortOrder.BY_NAME_ASC -> filtered.sortedBy { it.name }
            SortOrder.BY_SIZE_DESC -> filtered.sortedByDescending { it.size }
        }

        _imageItems.value = sorted.filter { it.type == MediaType.IMAGE }
        _videoItems.value = sorted.filter { it.type == MediaType.VIDEO }
        _audioItems.value = sorted.filter { it.type == MediaType.AUDIO }
    }

    fun onSearchQueryChanged(newQuery: String) { searchQuery.value = newQuery }
    fun onSortOrderChanged(newSortOrder: SortOrder) { sortOrder.value = newSortOrder }
    fun onTagFilterChanged(tag: String?) { tagFilter.value = tag }

    fun addTagToMediaItem(uri: String, tag: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val existingMetadata = mediaMetadataDao.getMetadataByUri(uri).first()
            val tags = existingMetadata?.tags?.toMutableList() ?: mutableListOf()
            if (!tags.contains(tag.trim()) && tag.isNotBlank()) {
                tags.add(tag.trim())
                mediaMetadataDao.insertOrUpdateMetadata(MediaMetadata(uri = uri, tags = tags))
                loadMedia()
            }
        }
    }

    fun loadMedia() {
        viewModelScope.launch(Dispatchers.IO) {
            val mediaFromStore = queryAllMediaStore()
            val metadataMap = mediaMetadataDao.getAllMetadata().first().associateBy { it.uri }

            allMediaItems = mediaFromStore.map { mediaStoreItem ->
                val metadata = metadataMap[mediaStoreItem.uri.toString()]
                mediaStoreItem.copy(tags = metadata?.tags ?: emptyList())
            }

            allTags.value = allMediaItems.flatMap { it.tags }.distinct().sorted()

            withContext(Dispatchers.Main) {
                updateFilteredLists(searchQuery.value, sortOrder.value, tagFilter.value)
            }
        }
    }

    private fun queryAllMediaStore(): List<MediaItem> {
        val allItems = mutableListOf<MediaItem>()
        allItems.addAll(queryMediaStore(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, MediaType.IMAGE))
        allItems.addAll(queryMediaStore(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, MediaType.VIDEO))
        allItems.addAll(queryMediaStore(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, MediaType.AUDIO))
        return allItems
    }

    private fun queryMediaStore(uri: Uri, type: MediaType): List<MediaItem> {
        val items = mutableListOf<MediaItem>()
        val context = getApplication<Application>().applicationContext

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.SIZE
        )

        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
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
