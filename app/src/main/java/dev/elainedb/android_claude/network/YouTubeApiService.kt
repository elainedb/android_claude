package dev.elainedb.android_claude.network

import dev.elainedb.android_claude.model.YouTubeSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface YouTubeApiService {

    @GET("search")
    suspend fun searchVideos(
        @Query("part") part: String = "snippet",
        @Query("channelId") channelId: String,
        @Query("maxResults") maxResults: Int = 10,
        @Query("order") order: String = "date",
        @Query("type") type: String = "video",
        @Query("key") apiKey: String
    ): YouTubeSearchResponse

    companion object {
        const val BASE_URL = "https://www.googleapis.com/youtube/v3/"
    }
}