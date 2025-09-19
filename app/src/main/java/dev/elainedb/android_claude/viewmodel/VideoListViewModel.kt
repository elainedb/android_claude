package dev.elainedb.android_claude.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.elainedb.android_claude.model.Video
import dev.elainedb.android_claude.repository.YouTubeRepository
import dev.elainedb.android_claude.repository.FilterOptions
import dev.elainedb.android_claude.repository.SortOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class VideoListViewModel(context: Context) : ViewModel() {

    private val repository = YouTubeRepository(context)

    private val _uiState = MutableStateFlow<VideoListUiState>(VideoListUiState.Loading)
    val uiState: StateFlow<VideoListUiState> = _uiState.asStateFlow()

    private val _filterOptions = MutableStateFlow(FilterOptions())
    private val _sortOption = MutableStateFlow(SortOption.PUBLICATION_DATE_NEWEST)

    private val _availableCountries = MutableStateFlow<List<String>>(emptyList())
    val availableCountries: StateFlow<List<String>> = _availableCountries.asStateFlow()

    private val _availableChannels = MutableStateFlow<List<String>>(emptyList())
    val availableChannels: StateFlow<List<String>> = _availableChannels.asStateFlow()

    private val _totalVideoCount = MutableStateFlow(0)
    val totalVideoCount: StateFlow<Int> = _totalVideoCount.asStateFlow()

    init {
        loadVideos()
        loadFilterOptions()
        observeVideoChanges()
        loadTotalVideoCount()
    }

    private fun loadTotalVideoCount() {
        viewModelScope.launch {
            try {
                _totalVideoCount.value = repository.getTotalVideoCount()
            } catch (e: Exception) {
                // Total count is not critical, continue without it
            }
        }
    }

    private fun loadVideos() {
        viewModelScope.launch {
            _uiState.value = VideoListUiState.Loading

            repository.getLatestVideos()
                .onSuccess { videos ->
                    if (videos.isEmpty()) {
                        _uiState.value = VideoListUiState.Empty
                    } else {
                        _uiState.value = VideoListUiState.Success(videos, _totalVideoCount.value)
                    }
                }
                .onFailure { exception ->
                    _uiState.value = VideoListUiState.Error(
                        exception.message ?: "Unknown error occurred"
                    )
                }
        }
    }

    private fun loadFilterOptions() {
        viewModelScope.launch {
            try {
                _availableCountries.value = repository.getAvailableCountries()
                _availableChannels.value = repository.getAvailableChannels()
            } catch (e: Exception) {
                // Filter options are not critical, continue without them
            }
        }
    }

    private fun observeVideoChanges() {
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(
                _filterOptions,
                _sortOption
            ) { filter, sort ->
                Pair(filter, sort)
            }.collectLatest { (filter, sort) ->
                repository.getVideos(filter, sort).collectLatest { videos ->
                    _uiState.value = if (videos.isEmpty()) {
                        VideoListUiState.Empty
                    } else {
                        VideoListUiState.Success(videos, _totalVideoCount.value)
                    }
                }
            }
        }
    }

    fun refreshVideos() {
        viewModelScope.launch {
            _uiState.value = VideoListUiState.Loading
            repository.refreshVideos()
                .onSuccess { videos ->
                    loadTotalVideoCount() // Reload total count after refresh
                    if (videos.isEmpty()) {
                        _uiState.value = VideoListUiState.Empty
                    } else {
                        _uiState.value = VideoListUiState.Success(videos, _totalVideoCount.value)
                    }
                    loadFilterOptions() // Reload filter options after refresh
                }
                .onFailure { exception ->
                    _uiState.value = VideoListUiState.Error(
                        exception.message ?: "Unknown error occurred"
                    )
                }
        }
    }

    fun applyFilter(filterOptions: FilterOptions) {
        _filterOptions.value = filterOptions
    }

    fun applySorting(sortOption: SortOption) {
        _sortOption.value = sortOption
    }

    fun clearFilters() {
        _filterOptions.value = FilterOptions()
    }
}

sealed class VideoListUiState {
    data object Loading : VideoListUiState()
    data object Empty : VideoListUiState()
    data class Success(
        val videos: List<Video>,
        val totalCount: Int = videos.size
    ) : VideoListUiState()
    data class Error(val message: String) : VideoListUiState()
}