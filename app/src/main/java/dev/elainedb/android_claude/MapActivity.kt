package dev.elainedb.android_claude

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import dev.elainedb.android_claude.database.VideoDatabase
import dev.elainedb.android_claude.database.toVideo
import dev.elainedb.android_claude.model.Video
import dev.elainedb.android_claude.repository.YouTubeRepository
import dev.elainedb.android_claude.ui.theme.AndroidClaudeTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class MapActivity : ComponentActivity() {

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, MapActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize OSMDroid configuration
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))

        setContent {
            AndroidClaudeTheme {
                MapScreen(
                    onBackClick = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var videos by remember { mutableStateOf<List<Video>>(emptyList()) }
    var selectedVideo by remember { mutableStateOf<Video?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    // Load videos with location data
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                val database = VideoDatabase.getDatabase(context)
                val videoEntities = withContext(Dispatchers.IO) {
                    database.videoDao().getVideosWithLocation()
                }
                videos = videoEntities.map { it.toVideo() }
                isLoading = false
            } catch (e: Exception) {
                isLoading = false
            }
        }
    }

    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    LaunchedEffect(showBottomSheet) {
        if (showBottomSheet) {
            bottomSheetState.show()
        } else {
            bottomSheetState.hide()
        }
    }

    LaunchedEffect(bottomSheetState.isVisible) {
        showBottomSheet = bottomSheetState.isVisible
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top App Bar
        TopAppBar(
            title = { Text("Video Map") },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        )

        // Map
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            if (isLoading) {
                // Loading indicator
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Loading map...")
                    }
                }
            } else if (videos.isEmpty()) {
                // No videos with location
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No videos with location data found",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Videos need GPS coordinates to appear on the map",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Map with videos
                AndroidView(
                    factory = { context ->
                        MapView(context).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)

                            // Add markers for each video
                            videos.forEach { video ->
                                if (video.locationLatitude != null && video.locationLongitude != null) {
                                    val marker = Marker(this).apply {
                                        position = GeoPoint(video.locationLatitude, video.locationLongitude)
                                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                        title = video.title
                                        snippet = video.channelName

                                        setOnMarkerClickListener { _, _ ->
                                            selectedVideo = video
                                            showBottomSheet = true
                                            true
                                        }
                                    }
                                    overlays.add(marker)
                                }
                            }

                            // Auto-fit all markers
                            if (videos.isNotEmpty()) {
                                val boundingBox = org.osmdroid.util.BoundingBox.fromGeoPoints(
                                    videos.mapNotNull { video ->
                                        if (video.locationLatitude != null && video.locationLongitude != null) {
                                            GeoPoint(video.locationLatitude, video.locationLongitude)
                                        } else null
                                    }
                                )

                                post {
                                    zoomToBoundingBox(boundingBox, true, 100)
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    // Bottom Sheet
    if (showBottomSheet && selectedVideo != null) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = bottomSheetState,
            modifier = Modifier.fillMaxHeight(0.25f)
        ) {
            VideoBottomSheetContent(
                video = selectedVideo!!,
                onVideoClick = { video ->
                    openYouTubeVideo(context, video.id)
                }
            )
        }
    }
}

@Composable
fun VideoBottomSheetContent(
    video: Video,
    onVideoClick: (Video) -> Unit
) {
    val context = LocalContext.current
    val repository = remember { YouTubeRepository(context) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable { onVideoClick(video) }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Thumbnail
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = "Video thumbnail",
                modifier = Modifier
                    .size(120.dp, 90.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Video info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = video.channelName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Published: ${repository.formatDateForDisplay(video.publishedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Location info
        if (video.locationLatitude != null && video.locationLongitude != null) {
            val locationText = buildString {
                if (!video.locationCity.isNullOrBlank() || !video.locationCountry.isNullOrBlank()) {
                    if (!video.locationCity.isNullOrBlank()) {
                        append(video.locationCity)
                    }
                    if (!video.locationCountry.isNullOrBlank()) {
                        if (!video.locationCity.isNullOrBlank()) append(", ")
                        append(video.locationCountry)
                    }
                    append(" • ")
                }
                append("GPS: ${String.format("%.4f", video.locationLatitude)}, ${String.format("%.4f", video.locationLongitude)}")
            }

            Text(
                text = locationText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Recording date if available
        if (!video.recordingDate.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Recorded: ${repository.formatDateForDisplay(video.recordingDate)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Tags if available
        if (video.tags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tags: ${video.tags.joinToString(", ")}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Click to watch hint
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "Tap to watch on YouTube",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(8.dp),
                fontWeight = FontWeight.Medium
            )
        }

        // Bottom padding to account for bottom sheet handle
        Spacer(modifier = Modifier.height(16.dp))
    }
}

private fun openYouTubeVideo(context: Context, videoId: String) {
    // Try to open in YouTube app first
    val youtubeAppIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$videoId"))
    youtubeAppIntent.setPackage("com.google.android.youtube")

    if (youtubeAppIntent.resolveActivity(context.packageManager) != null) {
        context.startActivity(youtubeAppIntent)
    } else {
        // Fallback to browser
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$videoId"))
        context.startActivity(browserIntent)
    }
}