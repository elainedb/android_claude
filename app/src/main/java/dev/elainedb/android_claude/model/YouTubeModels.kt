package dev.elainedb.android_claude.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class YouTubeSearchResponse(
    @SerialName("items")
    val items: List<YouTubeVideoItem> = emptyList(),
    @SerialName("nextPageToken")
    val nextPageToken: String? = null
)

@Serializable
data class YouTubeVideoItem(
    @SerialName("id")
    val id: YouTubeVideoId,
    @SerialName("snippet")
    val snippet: YouTubeVideoSnippet
)

@Serializable
data class YouTubeVideoId(
    @SerialName("videoId")
    val videoId: String
)

@Serializable
data class YouTubeVideoSnippet(
    @SerialName("title")
    val title: String,
    @SerialName("description")
    val description: String,
    @SerialName("channelTitle")
    val channelTitle: String,
    @SerialName("channelId")
    val channelId: String,
    @SerialName("publishedAt")
    val publishedAt: String,
    @SerialName("thumbnails")
    val thumbnails: YouTubeThumbnails,
    @SerialName("tags")
    val tags: List<String>? = null
)

@Serializable
data class YouTubeThumbnails(
    @SerialName("default")
    val default: YouTubeThumbnail? = null,
    @SerialName("medium")
    val medium: YouTubeThumbnail? = null,
    @SerialName("high")
    val high: YouTubeThumbnail? = null
)

@Serializable
data class YouTubeThumbnail(
    @SerialName("url")
    val url: String,
    @SerialName("width")
    val width: Int? = null,
    @SerialName("height")
    val height: Int? = null
)

// Models for YouTube Videos API (additional video details)
@Serializable
data class YouTubeVideosResponse(
    @SerialName("items")
    val items: List<YouTubeVideoDetails> = emptyList()
)

@Serializable
data class YouTubeVideoDetails(
    @SerialName("id")
    val id: String,
    @SerialName("snippet")
    val snippet: YouTubeVideoDetailsSnippet? = null,
    @SerialName("recordingDetails")
    val recordingDetails: YouTubeRecordingDetails? = null
)

@Serializable
data class YouTubeVideoDetailsSnippet(
    @SerialName("title")
    val title: String,
    @SerialName("description")
    val description: String,
    @SerialName("channelTitle")
    val channelTitle: String,
    @SerialName("channelId")
    val channelId: String,
    @SerialName("publishedAt")
    val publishedAt: String,
    @SerialName("thumbnails")
    val thumbnails: YouTubeThumbnails,
    @SerialName("tags")
    val tags: List<String>? = null
)

@Serializable
data class YouTubeRecordingDetails(
    @SerialName("recordingDate")
    val recordingDate: String? = null,
    @SerialName("location")
    val location: YouTubeLocation? = null
)

@Serializable
data class YouTubeLocation(
    @SerialName("latitude")
    val latitude: Double? = null,
    @SerialName("longitude")
    val longitude: Double? = null,
    @SerialName("altitude")
    val altitude: Double? = null
)

// Domain model for UI
data class Video(
    val id: String,
    val title: String,
    val channelName: String,
    val channelId: String,
    val publishedAt: String,
    val thumbnailUrl: String,
    val description: String,
    val tags: List<String> = emptyList(),
    val locationCity: String? = null,
    val locationCountry: String? = null,
    val locationLatitude: Double? = null,
    val locationLongitude: Double? = null,
    val recordingDate: String? = null
)

// Extension function to convert API model to domain model (basic search result)
fun YouTubeVideoItem.toVideo(): Video {
    return Video(
        id = id.videoId,
        title = snippet.title,
        channelName = snippet.channelTitle,
        channelId = snippet.channelId,
        publishedAt = snippet.publishedAt,
        thumbnailUrl = snippet.thumbnails.high?.url
            ?: snippet.thumbnails.medium?.url
            ?: snippet.thumbnails.default?.url
            ?: "",
        description = snippet.description,
        tags = snippet.tags ?: emptyList()
    )
}

// Extension function to merge additional video details
fun Video.mergeWithDetails(details: YouTubeVideoDetails): Video {
    return copy(
        tags = details.snippet?.tags ?: tags,
        locationLatitude = details.recordingDetails?.location?.latitude,
        locationLongitude = details.recordingDetails?.location?.longitude,
        recordingDate = details.recordingDetails?.recordingDate
    )
}