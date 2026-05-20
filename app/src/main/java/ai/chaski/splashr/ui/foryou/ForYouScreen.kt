package ai.chaski.splashr.ui.foryou

import ai.chaski.splashr.ai.AiSuggestionService
import ai.chaski.splashr.data.model.Photo
import ai.chaski.splashr.data.model.Topic
import ai.chaski.splashr.data.repository.PhotoRepository
import ai.chaski.splashr.ui.AppViewModelProvider
import ai.chaski.splashr.ui.components.EmptyState
import ai.chaski.splashr.ui.components.LoadingState
import ai.chaski.splashr.ui.components.PhotoStaggeredGrid
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ForYouViewModel(
    private val repository: PhotoRepository,
    private val aiService: AiSuggestionService,
) : ViewModel() {

    private val _interests = MutableStateFlow<Set<String>>(emptySet())
    val interests: StateFlow<Set<String>> = _interests.asStateFlow()

    val topics: StateFlow<List<Topic>> = repository.observeTopics()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val savedCount: StateFlow<Int> = repository.observeSavedPhotos()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /**
     * The AI ranking. Recomputed when interests or the saved-photo set change —
     * deliberately not on every heart tap, which would fire a fresh model call.
     */
    private val rankedRecommendations: StateFlow<List<Photo>> = combine(
        _interests,
        repository.observeSavedPhotos(),
        repository.observeAllPhotos(),
    ) { interests, saved, all ->
        aiService.recommendForYou(saved, interests.toList(), all)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * The displayed feed: the AI ranking with live saved state folded in, so a
     * tapped heart fills immediately without waiting on a re-ranking.
     */
    val recommendations: StateFlow<List<Photo>> = combine(
        rankedRecommendations,
        repository.observeSavedIds(),
    ) { ranked, savedIds ->
        ranked.map { photo -> photo.copy(isSaved = photo.id in savedIds) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toggleInterest(topic: String) {
        _interests.update { current ->
            if (topic in current) current - topic else current + topic
        }
    }

    fun toggleSave(photo: Photo) {
        viewModelScope.launch { repository.toggleSaved(photo) }
    }
}

@Composable
fun ForYouScreen(
    onPhotoClick: (String) -> Unit,
    viewModel: ForYouViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val interests by viewModel.interests.collectAsStateWithLifecycle()
    val topics by viewModel.topics.collectAsStateWithLifecycle()
    val recommendations by viewModel.recommendations.collectAsStateWithLifecycle()
    val savedCount by viewModel.savedCount.collectAsStateWithLifecycle()

    Scaffold(topBar = { TopAppBar(title = { Text("For You") }) }) { padding ->
        val hasSignal = interests.isNotEmpty() || savedCount > 0
        if (recommendations.isEmpty()) {
            Column(Modifier.padding(padding).fillMaxSize()) {
                InterestSection(
                    topics = topics,
                    interests = interests,
                    onToggle = viewModel::toggleInterest,
                    modifier = Modifier.padding(horizontal = 14.dp),
                )
                Box(Modifier.weight(1f)) {
                    if (hasSignal) {
                        LoadingState()
                    } else {
                        EmptyState(
                            icon = Icons.Outlined.Favorite,
                            title = "Build your For You feed",
                            message = "Pick a few interests above, or save photos you like — " +
                                "Splashr will recommend more from there.",
                        )
                    }
                }
            }
        } else {
            PhotoStaggeredGrid(
                photos = recommendations,
                onPhotoClick = onPhotoClick,
                onToggleSave = viewModel::toggleSave,
                modifier = Modifier.padding(padding),
                header = {
                    InterestSection(
                        topics = topics,
                        interests = interests,
                        onToggle = viewModel::toggleInterest,
                    )
                },
            )
        }
    }
}

@Composable
private fun InterestSection(
    topics: List<Topic>,
    interests: Set<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        Spacer(Modifier.height(8.dp))
        Text("Your interests", style = MaterialTheme.typography.titleLarge)
        Text(
            text = "Pick topics you like — recommendations update instantly.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            topics.forEach { topic ->
                FilterChip(
                    selected = topic.title in interests,
                    onClick = { onToggle(topic.title) },
                    label = { Text(topic.title) },
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Recommended for you", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(10.dp))
    }
}
