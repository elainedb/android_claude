package dev.elainedb.android_claude.repository

import android.content.Context
import android.util.Log
import dev.elainedb.android_claude.model.Video
import dev.elainedb.android_claude.model.toVideo
import dev.elainedb.android_claude.network.YouTubeApiService
import dev.elainedb.android_claude.utils.ConfigHelper
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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

class YouTubeRepository(private val context: Context) {

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

        // Channel IDs from requirements
        private val CHANNEL_IDS = listOf(
            "UCynoa1DjwnvHAowA_jiMEAQ",
            "UCK0KOjX3beyB9nzonls0cuw",
            "UCACkIrvrGAQ7kuc0hMVwvmA",
            "UCtWRAKKvOEA0CXOue9BG8ZA"
        )
    }

    suspend fun getLatestVideos(): Result<List<Video>> {
        return try {
            val apiKey = ConfigHelper.getYouTubeApiKey(context)
            Log.d(TAG, "Fetching videos from ${CHANNEL_IDS.size} channels")

            // Fetch videos from all channels in parallel
            val videosList = coroutineScope {
                CHANNEL_IDS.map { channelId ->
                    async {
                        try {
                            val response = apiService.searchVideos(
                                channelId = channelId,
                                apiKey = apiKey
                            )
                            response.items.map { it.toVideo() }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error fetching videos for channel $channelId", e)
                            emptyList<Video>()
                        }
                    }
                }.awaitAll()
            }

            // Flatten and sort by publication date (newest first)
            val allVideos = videosList.flatten()
                .sortedByDescending { video ->
                    try {
                        parsePublishedDate(video.publishedAt)
                    } catch (e: Exception) {
                        Log.w(TAG, "Error parsing date for video: ${video.title}", e)
                        Date(0) // Default to epoch if parsing fails
                    }
                }

            Log.d(TAG, "Successfully fetched ${allVideos.size} videos")
            Result.success(allVideos)

        } catch (e: Exception) {
            Log.e(TAG, "Error fetching latest videos", e)
            Result.failure(e)
        }
    }

    private fun parsePublishedDate(dateString: String): Date {
        // YouTube API returns dates in RFC 3339 format: 2023-12-25T10:30:00Z
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        format.timeZone = TimeZone.getTimeZone("UTC")
        return format.parse(dateString) ?: Date(0)
    }

    fun formatDateForDisplay(dateString: String): String {
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