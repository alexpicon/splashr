package ai.chaski.splashr.ui.search

import ai.chaski.splashr.data.model.Orientation
import ai.chaski.splashr.data.model.Photo
import ai.chaski.splashr.data.model.SearchFilter
import ai.chaski.splashr.data.model.Topic
import ai.chaski.splashr.data.repository.PhotoRepository
import ai.chaski.splashr.data.sample.SampleData
import ai.chaski.splashr.ui.AppViewModelProvider
import ai.chaski.splashr.ui.components.EmptyState
import ai.chaski.splashr.ui.components.LoadingState
import ai.chaski.splashr.ui.components.PhotoStaggeredGrid
import ai.chaski.splashr.ui.navigation.Routes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SearchViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: PhotoRepository,
) : ViewModel() {

    private val _filter = MutableStateFlow(
        SearchFilter(
            query = savedStateHandle.get<String>(Routes.ARG_QUERY).orEmpty(),
            topic = savedStateHandle.get<String>(Routes.ARG_TOPIC),
        ),
    )
    val filter: StateFlow<SearchFilter> = _filter.asStateFlow()

    private val _rawResults = MutableStateFlow<List<Photo>>(emptyList())
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    /** Results with live "saved" state folded in from the local database. */
    val results: StateFlow<List<Photo>> =
        combine(_rawResults, repository.observeSavedIds()) { photos, savedIds ->
            photos.map { it.copy(isSaved = it.id in savedIds) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val topics: StateFlow<List<Topic>> = repository.observeTopics()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val colors: List<String> = SampleData.colors

    init {
        runSearch()
    }

    fun updateQuery(query: String) = update { it.copy(query = query) }
    fun setTopic(topic: String?) = update { it.copy(topic = topic) }
    fun setOrientation(orientation: Orientation) = update { it.copy(orientation = orientation) }
    fun setColor(color: String?) = update { it.copy(color = color) }
    fun clearFilters() = update { SearchFilter(query = it.query) }

    fun toggleSave(photo: Photo) {
        viewModelScope.launch { repository.toggleSaved(photo) }
    }

    private fun update(transform: (SearchFilter) -> SearchFilter) {
        _filter.update(transform)
        runSearch()
    }

    private fun runSearch() {
        viewModelScope.launch {
            _isSearching.value = true
            _rawResults.value = repository.search(_filter.value)
            _isSearching.value = false
        }
    }
}

@Composable
fun SearchScreen(
    onPhotoClick: (String) -> Unit,
    viewModel: SearchViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    val topics by viewModel.topics.collectAsStateWithLifecycle()

    Scaffold(topBar = { TopAppBar(title = { Text("Search") }) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = filter.query,
                onValueChange = viewModel::updateQuery,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                placeholder = { Text("Search photos, tags, people") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = {
                    if (filter.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateQuery("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
            )
            Spacer(Modifier.height(8.dp))
            FiltersBlock(filter = filter, topics = topics, viewModel = viewModel)
            HorizontalDivider()
            Box(Modifier.weight(1f)) {
                when {
                    isSearching -> LoadingState()
                    results.isEmpty() -> EmptyState(
                        icon = Icons.Outlined.SearchOff,
                        title = "No photos found",
                        message = "Try a different search, or clear some of the filters above.",
                    )
                    else -> PhotoStaggeredGrid(
                        photos = results,
                        onPhotoClick = onPhotoClick,
                        onToggleSave = viewModel::toggleSave,
                    )
                }
            }
        }
    }
}

@Composable
private fun FiltersBlock(
    filter: SearchFilter,
    topics: List<Topic>,
    viewModel: SearchViewModel,
) {
    Column(Modifier.padding(horizontal = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Filters",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            if (filter.activeFilterCount > 0) {
                TextButton(onClick = viewModel::clearFilters) {
                    Text("Clear (${filter.activeFilterCount})")
                }
            }
        }

        ChipGroupLabel("Topic")
        LazyRow {
            item {
                SplashrFilterChip(
                    label = "All",
                    selected = filter.topic == null,
                    onClick = { viewModel.setTopic(null) },
                )
            }
            items(topics, key = { it.id }) { topic ->
                SplashrFilterChip(
                    label = topic.title,
                    selected = filter.topic == topic.title,
                    onClick = {
                        viewModel.setTopic(topic.title.takeIf { filter.topic != topic.title })
                    },
                )
            }
        }

        ChipGroupLabel("Orientation")
        LazyRow {
            items(Orientation.entries.toList()) { orientation ->
                SplashrFilterChip(
                    label = orientation.label,
                    selected = filter.orientation == orientation,
                    onClick = { viewModel.setOrientation(orientation) },
                )
            }
        }

        ChipGroupLabel("Colour")
        LazyRow {
            items(viewModel.colors) { color ->
                SplashrFilterChip(
                    label = color.replaceFirstChar { it.uppercase() },
                    selected = filter.color == color,
                    onClick = { viewModel.setColor(color.takeIf { filter.color != color }) },
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ChipGroupLabel(text: String) {
    Spacer(Modifier.height(8.dp))
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun SplashrFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = Modifier.padding(end = 8.dp),
    )
}
