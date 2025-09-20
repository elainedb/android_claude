package dev.elainedb.android_claude.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.elainedb.android_claude.model.Video
import dev.elainedb.android_claude.repository.YouTubeRepository
import dev.elainedb.android_claude.repository.FilterOptions
import dev.elainedb.android_claude.repository.SortOption
import dev.elainedb.android_claude.viewmodel.VideoListUiState
import dev.elainedb.android_claude.viewmodel.VideoListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoListScreen(
    viewModel: VideoListViewModel,
    onLogout: () -> Unit,
    onViewMap: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val availableCountries by viewModel.availableCountries.collectAsState()
    val availableChannels by viewModel.availableChannels.collectAsState()
    val context = LocalContext.current

    var showFilterDialog by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    var currentFilterOptions by remember { mutableStateOf(FilterOptions()) }
    var currentSortOption by remember { mutableStateOf(SortOption.PUBLICATION_DATE_NEWEST) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top bar with logout button
        TopAppBar(
            title = { Text("YouTube Videos") },
            actions = {
                TextButton(onClick = onLogout) {
                    Text("Logout")
                }
            }
        )

        // Control buttons - first row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Refresh Button
            FilledTonalButton(
                onClick = { viewModel.refreshVideos() },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh"
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Refresh")
            }

            // View Map Button
            FilledTonalButton(
                onClick = onViewMap,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "View Map"
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("View Map")
            }
        }

        // Control buttons - second row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Filter Button
            OutlinedButton(
                onClick = { showFilterDialog = true },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Filter"
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Filter")
            }

            // Sort Button
            OutlinedButton(
                onClick = { showSortDialog = true },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.List,
                    contentDescription = "Sort"
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Sort")
            }
        }

        // Content based on state
        when (val state = uiState) {
            is VideoListUiState.Loading -> {
                LoadingContent()
            }
            is VideoListUiState.Empty -> {
                EmptyContent(onRefresh = { viewModel.refreshVideos() })
            }
            is VideoListUiState.Success -> {
                VideoListWithCount(
                    videos = state.videos,
                    totalCount = state.totalCount,
                    onVideoClick = { video ->
                        openYouTubeVideo(context, video.id)
                    },
                    onRefresh = { viewModel.refreshVideos() }
                )
            }
            is VideoListUiState.Error -> {
                ErrorContent(
                    message = state.message,
                    onRetry = { viewModel.refreshVideos() }
                )
            }
        }
    }

    // Filter Dialog
    FilterDialog(
        isOpen = showFilterDialog,
        currentFilter = currentFilterOptions,
        availableChannels = availableChannels,
        availableCountries = availableCountries,
        onDismiss = { showFilterDialog = false },
        onApplyFilter = { filterOptions ->
            currentFilterOptions = filterOptions
            viewModel.applyFilter(filterOptions)
        }
    )

    // Sort Dialog
    SortDialog(
        isOpen = showSortDialog,
        currentSortOption = currentSortOption,
        onDismiss = { showSortDialog = false },
        onApplySort = { sortOption ->
            currentSortOption = sortOption
            viewModel.applySorting(sortOption)
        }
    )
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Loading videos...")
        }
    }
}

@Composable
private fun EmptyContent(onRefresh: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("No videos found")
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRefresh) {
                Text("Refresh")
            }
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Error: $message",
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@Composable
private fun VideoListWithCount(
    videos: List<Video>,
    totalCount: Int,
    onVideoClick: (Video) -> Unit,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Video count header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            val countText = if (videos.size == totalCount) {
                "${videos.size} video${if (videos.size != 1) "s" else ""}"
            } else {
                "${videos.size} of $totalCount video${if (totalCount != 1) "s" else ""}"
            }
            Text(
                text = countText,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Video list
        VideoList(
            videos = videos,
            onVideoClick = onVideoClick,
            onRefresh = onRefresh
        )
    }
}

@Composable
private fun VideoList(
    videos: List<Video>,
    onVideoClick: (Video) -> Unit,
    onRefresh: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(videos) { video ->
            VideoItem(
                video = video,
                onClick = { onVideoClick(video) }
            )
        }
    }
}

@Composable
private fun VideoItem(
    video: Video,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { YouTubeRepository(context) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Thumbnail
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = "Video thumbnail",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Title
            Text(
                text = video.title,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Channel name
            Text(
                text = "Channel: ${video.channelName}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Published date
            Text(
                text = "Published: ${repository.formatDateForDisplay(video.publishedAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Recording date (if available)
            if (!video.recordingDate.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Recorded: ${repository.formatDateForDisplay(video.recordingDate)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Tags (if available)
            if (video.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tags:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(video.tags) { tag ->
                        AssistChip(
                            onClick = { },
                            label = {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        )
                    }
                }
            }

            // Location (if available)
            if (video.locationLatitude != null && video.locationLongitude != null) {
                Spacer(modifier = Modifier.height(8.dp))

                // Display resolved location name if available
                val hasLocationName = !video.locationCity.isNullOrBlank() || !video.locationCountry.isNullOrBlank()
                if (hasLocationName) {
                    val locationNameText = buildString {
                        append("Location: ")
                        if (!video.locationCity.isNullOrBlank()) {
                            append(video.locationCity)
                        }
                        if (!video.locationCountry.isNullOrBlank()) {
                            if (!video.locationCity.isNullOrBlank()) append(", ")
                            append(video.locationCountry)
                        }
                    }
                    Text(
                        text = locationNameText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }

                // Always display GPS coordinates if available
                Text(
                    text = "GPS: ${String.format("%.4f", video.locationLatitude)}, ${String.format("%.4f", video.locationLongitude)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}

private fun openYouTubeVideo(context: android.content.Context, videoId: String) {
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