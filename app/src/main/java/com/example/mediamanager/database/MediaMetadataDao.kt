package com.example.mediamanager.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaMetadataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateMetadata(metadata: MediaMetadata)

    @Query("SELECT * FROM media_metadata WHERE uri = :uri")
    fun getMetadataByUri(uri: String): Flow<MediaMetadata?>

    @Query("SELECT * FROM media_metadata")
    fun getAllMetadata(): Flow<List<MediaMetadata>>
}
