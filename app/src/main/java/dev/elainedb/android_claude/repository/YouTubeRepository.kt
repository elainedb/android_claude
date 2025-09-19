package dev.elainedb.android_claude.repository

import android.content.Context
import android.util.Log
import dev.elainedb.android_claude.database.VideoDatabase
import dev.elainedb.android_claude.database.toEntity
import dev.elainedb.android_claude.database.toVideo
import dev.elainedb.android_claude.model.Video
import dev.elainedb.android_claude.model.toVideo
import dev.elainedb.android_claude.model.mergeWithDetails
import dev.elainedb.android_claude.network.YouTubeApiService
import dev.elainedb.android_claude.utils.ConfigHelper
import dev.elainedb.android_claude.utils.LocationUtils
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import android.util.Base64

enum class SortOption {
    PUBLICATION_DATE_NEWEST,
    PUBLICATION_DATE_OLDEST,
    RECORDING_DATE_NEWEST,
    RECORDING_DATE_OLDEST
}

data class FilterOptions(
    val channelName: String? = null,
    val country: String? = null
)

class YouTubeRepository(private val context: Context) {

    private val database = VideoDatabase.getDatabase(context)
    private val videoDao = database.videoDao()

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // Android package header interceptor for YouTube API
    private val androidHeadersInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val newRequest = originalRequest.newBuilder()
            .addHeader("X-Android-Package", context.packageName)
            .addHeader("X-Android-Cert", getAppSignature())
            .build()
        chain.proceed(newRequest)
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(androidHeadersInterceptor)
        .addInterceptor(loggingInterceptor)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(YouTubeApiService.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    private val apiService = retrofit.create(YouTubeApiService::class.java)

    companion object {
        private const val TAG = "YouTubeRepository"
        private const val CACHE_EXPIRY_HOURS = 24
        private const val MAX_VIDEO_IDS_PER_REQUEST = 50

        // Channel IDs from requirements
        private val CHANNEL_IDS = listOf(
            "UCynoa1DjwnvHAowA_jiMEAQ",
            "UCK0KOjX3beyB9nzonls0cuw",
            "UCACkIrvrGAQ7kuc0hMVwvmA",
            "UCtWRAKKvOEA0CXOue9BG8ZA"
        )
    }

    // Get videos with filtering and sorting options
    fun getVideos(filterOptions: FilterOptions = FilterOptions(), sortOption: SortOption = SortOption.PUBLICATION_DATE_NEWEST): Flow<List<Video>> {
        return videoDao.getVideosWithFiltersAndSort(
            channelName = filterOptions.channelName,
            country = filterOptions.country,
            sortBy = sortOption.name
        ).map { entities -> entities.map { it.toVideo() } }
    }

    // Get available filter options
    suspend fun getAvailableCountries(): List<String> = videoDao.getDistinctCountries()
    suspend fun getAvailableChannels(): List<String> = videoDao.getDistinctChannels()

    // Get total video count
    suspend fun getTotalVideoCount(): Int = videoDao.getTotalVideoCount()

    // Refresh videos - force API call and update cache
    suspend fun refreshVideos(): Result<List<Video>> {
        return try {
            // Test reverse geocoding with known coordinates
            Log.i(TAG, "Testing reverse geocoding functionality...")
            val testResult = LocationUtils.testReverseGeocoding(context)
            Log.i(TAG, "Test geocoding result: city=${testResult.first}, country=${testResult.second}")

            val videos = fetchVideosFromApi()
            if (videos.isNotEmpty()) {
                videoDao.insertVideos(videos.map { it.toEntity() })
            }
            Result.success(videos)
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing videos", e)
            Result.failure(e)
        }
    }

    // Get videos - check cache first, then API if needed
    suspend fun getLatestVideos(): Result<List<Video>> {
        return try {
            // Check if we have fresh cached data
            val cacheThreshold = System.currentTimeMillis() - (CACHE_EXPIRY_HOURS * 60 * 60 * 1000)
            val cachedVideos = videoDao.getVideosNewerThan(cacheThreshold)

            if (cachedVideos.isNotEmpty()) {
                Log.d(TAG, "Using cached videos: ${cachedVideos.size}")
                Result.success(cachedVideos.map { it.toVideo() })
            } else {
                Log.d(TAG, "Cache expired or empty, fetching from API")
                refreshVideos()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting latest videos", e)
            Result.failure(e)
        }
    }

    private suspend fun fetchVideosFromApi(): List<Video> {
        val apiKey = ConfigHelper.getYouTubeApiKey(context)
        Log.d(TAG, "Fetching videos from ${CHANNEL_IDS.size} channels")

        // Fetch basic video data from all channels in parallel
        val videosList = coroutineScope {
            CHANNEL_IDS.map { channelId ->
                async {
                    fetchAllVideosFromChannel(channelId, apiKey)
                }
            }.awaitAll()
        }

        val allVideos = videosList.flatten()
        Log.i(TAG, "Total videos fetched from all channels: ${allVideos.size}")

        // Fetch additional details for all videos
        val enhancedVideos = if (allVideos.isNotEmpty()) {
            fetchVideoDetails(allVideos, apiKey)
        } else {
            allVideos
        }

        // Sort by publication date (newest first)
        val sortedVideos = enhancedVideos.sortedByDescending { video ->
            try {
                parsePublishedDate(video.publishedAt)
            } catch (e: Exception) {
                Log.w(TAG, "Error parsing date for video: ${video.title}", e)
                Date(0)
            }
        }

        Log.d(TAG, "Successfully fetched ${sortedVideos.size} videos with enhanced details")
        return sortedVideos
    }

    private suspend fun fetchAllVideosFromChannel(channelId: String, apiKey: String): List<Video> {
        val allVideos = mutableListOf<Video>()
        var nextPageToken: String? = null
        var pageCount = 0
        val maxPages = 5 // Safety limit to prevent infinite loops

        Log.d(TAG, "Starting to fetch all videos from channel: $channelId")

        do {
            try {
                pageCount++
                Log.d(TAG, "Fetching page $pageCount for channel $channelId (token: $nextPageToken)")

                val response = apiService.searchVideos(
                    channelId = channelId,
                    maxResults = 50, // YouTube API max per request
                    pageToken = nextPageToken,
                    apiKey = apiKey
                )

                val videos = response.items.map { it.toVideo() }
                allVideos.addAll(videos)
                nextPageToken = response.nextPageToken

                Log.d(TAG, "Page $pageCount for channel $channelId: got ${videos.size} videos, total so far: ${allVideos.size}")
                Log.d(TAG, "Next page token: $nextPageToken")

            } catch (e: Exception) {
                Log.e(TAG, "Error fetching page $pageCount for channel $channelId", e)
                break // Stop pagination on error
            }
        } while (nextPageToken != null && pageCount < maxPages)

        Log.i(TAG, "Finished fetching from channel $channelId: ${allVideos.size} total videos in $pageCount pages")
        return allVideos
    }

    private suspend fun fetchVideoDetails(videos: List<Video>, apiKey: String): List<Video> {
        return try {
            // Split video IDs into batches to avoid URL length limits
            val videoIdBatches = videos.map { it.id }.chunked(MAX_VIDEO_IDS_PER_REQUEST)

            val detailsList = coroutineScope {
                videoIdBatches.map { batch ->
                    async {
                        try {
                            Log.d(TAG, "Fetching video details for batch: ${batch.joinToString(",")}")
                            val response = apiService.getVideoDetails(
                                videoIds = batch.joinToString(","),
                                apiKey = apiKey
                            )
                            Log.d(TAG, "Got ${response.items.size} video details from API")
                            response.items.forEach { item ->
                                Log.d(TAG, "Video details for ${item.id}:")
                                Log.d(TAG, "  - snippet: ${item.snippet != null}")
                                Log.d(TAG, "  - recordingDetails: ${item.recordingDetails != null}")
                                if (item.recordingDetails?.location != null) {
                                    Log.d(TAG, "  - location: lat=${item.recordingDetails.location.latitude}, lng=${item.recordingDetails.location.longitude}")
                                } else {
                                    Log.d(TAG, "  - no location data")
                                }
                            }
                            response.items
                        } catch (e: Exception) {
                            Log.e(TAG, "Error fetching video details for batch: $batch", e)
                            emptyList()
                        }
                    }
                }.awaitAll()
            }

            val allDetails = detailsList.flatten()
            val detailsMap = allDetails.associateBy { it.id }

            // Enhance videos with additional details and location info
            coroutineScope {
                videos.map { video ->
                    async {
                        val details = detailsMap[video.id]
                        var enhancedVideo = if (details != null) {
                            Log.d(TAG, "Found details for video ${video.id}: ${video.title}")
                            val merged = video.mergeWithDetails(details)
                            Log.d(TAG, "Video ${video.id} location data: lat=${merged.locationLatitude}, lng=${merged.locationLongitude}")
                            merged
                        } else {
                            Log.d(TAG, "No details found for video ${video.id}: ${video.title}")
                            video
                        }

                        // If we have coordinates, try to resolve city/country
                        if (enhancedVideo.locationLatitude != null && enhancedVideo.locationLongitude != null) {
                            Log.d(TAG, "Attempting reverse geocoding for video ${video.title} at (${enhancedVideo.locationLatitude}, ${enhancedVideo.locationLongitude})")
                            val (city, country) = LocationUtils.getLocationFromCoordinates(
                                context,
                                enhancedVideo.locationLatitude!!,
                                enhancedVideo.locationLongitude!!
                            )
                            Log.d(TAG, "Reverse geocoding result for ${video.title}: city=$city, country=$country")
                            enhancedVideo = enhancedVideo.copy(
                                locationCity = city,
                                locationCountry = country
                            )
                        } else {
                            Log.d(TAG, "Video ${video.title} has no GPS coordinates")
                        }

                        enhancedVideo
                    }
                }.awaitAll()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching video details", e)
            videos // Return original videos if enhancement fails
        }
    }

    private fun parsePublishedDate(dateString: String): Date {
        // YouTube API returns dates in RFC 3339 format: 2023-12-25T10:30:00Z
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        format.timeZone = TimeZone.getTimeZone("UTC")
        return format.parse(dateString) ?: Date(0)
    }

    fun formatDateForDisplay(dateString: String?): String {
        if (dateString.isNullOrBlank()) return ""
        return try {
            val date = parsePublishedDate(dateString)
            val displayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            displayFormat.format(date)
        } catch (e: Exception) {
            Log.w(TAG, "Error formatting date: $dateString", e)
            dateString // Return original string if formatting fails
        }
    }

    private fun getAppSignature(): String {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNATURES
                )
            }

            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }

            signatures?.firstOrNull()?.let { signature ->
                val md = MessageDigest.getInstance("SHA1")
                md.update(signature.toByteArray())
                val digest = md.digest()
                val sha1 = Base64.encodeToString(digest, Base64.NO_WRAP)

                // Also compute SHA1 in hex format for Google Console
                val hexSha1 = digest.joinToString(":") {
                    String.format("%02X", it)
                }
                Log.d(TAG, "App SHA1 (Base64): $sha1")
                Log.d(TAG, "App SHA1 (Hex): $hexSha1")

                sha1
            } ?: ""
        } catch (e: Exception) {
            Log.e(TAG, "Error getting app signature", e)
            ""
        }
    }
}