package dev.elainedb.android_claude.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.elainedb.android_claude.model.Video
import dev.elainedb.android_claude.repository.YouTubeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VideoListViewModel(context: Context) : ViewModel() {

    private val repository = YouTubeRepository(context)

    private val _uiState = MutableStateFlow<VideoListUiState>(VideoListUiState.Loading)
    val uiState: StateFlow<VideoListUiState> = _uiState.asStateFlow()

    init {
        loadVideos()
    }

    fun loadVideos() {
        viewModelScope.launch {
            _uiState.value = VideoListUiState.Loading

            repository.getLatestVideos()
                .onSuccess { videos ->
                    if (videos.isEmpty()) {
                        _uiState.value = VideoListUiState.Empty
                    } else {
                        _uiState.value = VideoListUiState.Success(videos)
                    }
                }
                .onFailure { exception ->
                    _uiState.value = VideoListUiState.Error(
                        exception.message ?: "Unknown error occurred"
                    )
                }
        }
    }

    fun refreshVideos() {
        loadVideos()
    }
}

sealed class VideoListUiState {
    data object Loading : VideoListUiState()
    data object Empty : VideoListUiState()
    data class Success(val videos: List<Video>) : VideoListUiState()
    data class Error(val message: String) : VideoListUiState()
}