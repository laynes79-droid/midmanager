package com.example.mediamanager

import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import java.lang.reflect.Field

@ExperimentalCoroutinesApi
class MediaViewModelTest {

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private lateinit var viewModel: MediaViewModel
    private val mockUri: Uri = mock()

    private val testMediaItems = listOf(
        MediaItem(mockUri, "cat_photo.jpg", MediaType.IMAGE, 100L, 1024),
        MediaItem(mockUri, "dog_video.mp4", MediaType.VIDEO, 300L, 5120),
        MediaItem(mockUri, "cat_video.mp4", MediaType.VIDEO, 200L, 2048),
        MediaItem(mockUri, "bird_song.mp3", MediaType.AUDIO, 500L, 512),
        MediaItem(mockUri, "animal_sounds.mp3", MediaType.AUDIO, 400L, 256)
    )

    @Before
    fun setup() {
        viewModel = MediaViewModel()
        // Use reflection to set the private StateFlow for testing
        val field: Field = viewModel.javaClass.getDeclaredField("_allMediaItems")
        field.isAccessible = true
        field.set(viewModel, MutableStateFlow(testMediaItems))
    }

    @Test
    fun `search query filters image list correctly`() = runBlocking {
        viewModel.onSearchQueryChanged("cat")
        val images = viewModel.imageItems.first()
        assertThat(images).hasSize(1)
        assertThat(images.first().name).isEqualTo("cat_photo.jpg")
    }

    @Test
    fun `search query filters video list correctly`() = runBlocking {
        viewModel.onSearchQueryChanged("cat")
        val videos = viewModel.videoItems.first()
        assertThat(videos).hasSize(1)
        assertThat(videos.first().name).isEqualTo("cat_video.mp4")
    }

    @Test
    fun `sort order BY_DATE_DESC sorts items correctly`() = runBlocking {
        viewModel.onSortOrderChanged(SortOrder.BY_DATE_DESC)
        val audios = viewModel.audioItems.first()
        assertThat(audios.map { it.dateAdded }).containsExactly(500L, 400L).inOrder()
    }

    @Test
    fun `sort order BY_NAME_ASC sorts items correctly`() = runBlocking {
        viewModel.onSortOrderChanged(SortOrder.BY_NAME_ASC)
        val audios = viewModel.audioItems.first()
        assertThat(audios.map { it.name }).containsExactly("animal_sounds.mp3", "bird_song.mp3").inOrder()
    }

    @Test
    fun `sort order BY_SIZE_DESC sorts items correctly`() = runBlocking {
        viewModel.onSortOrderChanged(SortOrder.BY_SIZE_DESC)
        val videos = viewModel.videoItems.first()
        assertThat(videos.map { it.size }).containsExactly(5120L, 2048L).inOrder()
    }
}
