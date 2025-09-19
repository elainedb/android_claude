package dev.elainedb.android_claude.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {

    // Combined filter and sort queries
    @Query("""
        SELECT * FROM videos
        WHERE (:channelName IS NULL OR channelName = :channelName)
        AND (:country IS NULL OR locationCountry = :country)
        ORDER BY
        CASE WHEN :sortBy = 'PUBLICATION_DATE_NEWEST' THEN publishedAt END DESC,
        CASE WHEN :sortBy = 'PUBLICATION_DATE_OLDEST' THEN publishedAt END ASC,
        CASE WHEN :sortBy = 'RECORDING_DATE_NEWEST' THEN recordingDate END DESC,
        CASE WHEN :sortBy = 'RECORDING_DATE_OLDEST' THEN recordingDate END ASC
    """)
    fun getVideosWithFiltersAndSort(
        channelName: String?,
        country: String?,
        sortBy: String
    ): Flow<List<VideoEntity>>

    @Query("SELECT DISTINCT locationCountry FROM videos WHERE locationCountry IS NOT NULL AND locationCountry != ''")
    suspend fun getDistinctCountries(): List<String>

    @Query("SELECT DISTINCT channelName FROM videos")
    suspend fun getDistinctChannels(): List<String>

    @Query("SELECT * FROM videos WHERE cacheTimestamp > :threshold")
    suspend fun getVideosNewerThan(threshold: Long): List<VideoEntity>

    @Query("SELECT COUNT(*) FROM videos")
    suspend fun getTotalVideoCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideos(videos: List<VideoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: VideoEntity)

    @Update
    suspend fun updateVideo(video: VideoEntity)

    @Delete
    suspend fun deleteVideo(video: VideoEntity)

    @Query("DELETE FROM videos")
    suspend fun deleteAllVideos()

    @Query("DELETE FROM videos WHERE cacheTimestamp < :threshold")
    suspend fun deleteOldVideos(threshold: Long)
}