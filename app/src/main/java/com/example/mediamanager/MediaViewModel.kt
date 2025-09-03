package com.example.mediamanager

import android.app.Application
import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mediamanager.database.AppDatabase
import com.example.mediamanager.database.MediaMetadata
import android.content.ContentValues
import android.os.Environment
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

    fun deleteMediaItem(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                getApplication<Application>().contentResolver.delete(uri, null, null)
                loadMedia() // Refresh the list after deletion
            } catch (e: Exception) {
                e.printStackTrace()
                // TODO: Handle exceptions, e.g., show an error message to the user
            }
        }
    }

    fun duplicateMediaItem(item: MediaItem) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>().applicationContext
            val contentResolver = context.contentResolver

            val newName = "copy_of_${item.name}"
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, newName)
                put(MediaStore.MediaColumns.MIME_TYPE, contentResolver.getType(item.uri))
                // Copy to the same primary directory (e.g., Pictures, Movies)
                when (item.type) {
                    MediaType.IMAGE -> put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                    MediaType.VIDEO -> put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MOVIES)
                    MediaType.AUDIO -> put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MUSIC)
                }
            }

            val collectionUri = when(item.type) {
                MediaType.IMAGE -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                MediaType.VIDEO -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                MediaType.AUDIO -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }

            val newFileUri = contentResolver.insert(collectionUri, contentValues)

            if (newFileUri != null) {
                try {
                    contentResolver.openInputStream(item.uri)?.use { inputStream ->
                        contentResolver.openOutputStream(newFileUri)?.use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    loadMedia() // Refresh to show the new file
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Clean up if copy fails
                    contentResolver.delete(newFileUri, null, null)
                }
            }
        }
    }

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
