package dev.elainedb.android_claude.network

import dev.elainedb.android_claude.model.YouTubeSearchResponse
import dev.elainedb.android_claude.model.YouTubeVideosResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface YouTubeApiService {

    @GET("search")
    suspend fun searchVideos(
        @Query("part") part: String = "snippet",
        @Query("channelId") channelId: String,
        @Query("maxResults") maxResults: Int = 50, // YouTube API max per request
        @Query("order") order: String = "date",
        @Query("type") type: String = "video",
        @Query("pageToken") pageToken: String? = null,
        @Query("key") apiKey: String
    ): YouTubeSearchResponse

    @GET("videos")
    suspend fun getVideoDetails(
        @Query("part") part: String = "snippet,recordingDetails",
        @Query("id") videoIds: String, // Comma-separated video IDs
        @Query("key") apiKey: String
    ): YouTubeVideosResponse

    companion object {
        const val BASE_URL = "https://www.googleapis.com/youtube/v3/"
    }
}