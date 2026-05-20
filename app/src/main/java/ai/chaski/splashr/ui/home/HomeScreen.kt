package ai.chaski.splashr.ui.home

import ai.chaski.splashr.data.model.Photo
import ai.chaski.splashr.data.model.Topic
import ai.chaski.splashr.data.repository.PhotoRepository
import ai.chaski.splashr.ui.AppViewModelProvider
import ai.chaski.splashr.ui.components.LoadingState
import ai.chaski.splashr.ui.components.PhotoStaggeredGrid
import ai.chaski.splashr.ui.components.SectionHeader
import ai.chaski.splashr.ui.components.SplashrAsyncImage
import ai.chaski.splashr.ui.components.TopicCard
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val topics: List<Topic> = emptyList(),
    val featured: List<Photo> = emptyList(),
    val trending: List<Photo> = emptyList(),
    val isLoading: Boolean = true,
)

class HomeViewModel(private val repository: PhotoRepository) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        repository.observeTopics(),
        repository.observeFeaturedPhotos(),
        repository.observeTrendingPhotos(),
    ) { topics, featured, trending ->
        HomeUiState(topics, featured, trending, isLoading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun toggleSave(photo: Photo) {
        viewModelScope.launch { repository.toggleSaved(photo) }
    }
}

@Composable
fun HomeScreen(
    onPhotoClick: (String) -> Unit,
    onTopicClick: (String) -> Unit,
    viewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // No top app bar — the "Discover" header below carries the screen identity,
    // keeping more vertical space for actual photography.
    Scaffold { padding ->
        if (state.isLoading) {
            LoadingState(Modifier.padding(padding))
        } else {
            PhotoStaggeredGrid(
                photos = state.trending,
                onPhotoClick = onPhotoClick,
                onToggleSave = viewModel::toggleSave,
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 14.dp),
                header = {
                    HomeHeader(
                        state = state,
                        onTopicClick = onTopicClick,
                        onPhotoClick = onPhotoClick,
                    )
                },
            )
        }
    }
}

@Composable
private fun HomeHeader(
    state: HomeUiState,
    onTopicClick: (String) -> Unit,
    onPhotoClick: (String) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
        Spacer(Modifier.height(8.dp))
        Text("Discover", style = MaterialTheme.typography.headlineMedium)
        Text(
            text = "Beautiful photography, ready to explore.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(18.dp))
        SectionHeader("Trending topics")
        Spacer(Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(state.topics, key = { it.id }) { topic ->
                TopicCard(topic = topic, onClick = { onTopicClick(topic.title) })
            }
        }

        Spacer(Modifier.height(20.dp))
        SectionHeader("Featured")
        Spacer(Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(state.featured, key = { it.id }) { photo ->
                FeaturedCard(photo = photo, onClick = { onPhotoClick(photo.id) })
            }
        }

        Spacer(Modifier.height(20.dp))
        SectionHeader("Trending now", subtitle = "Fresh photos picked across every topic")
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun FeaturedCard(photo: Photo, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.size(width = 248.dp, height = 156.dp),
    ) {
        Box {
            SplashrAsyncImage(photo.imageUrl, photo.title, Modifier.matchParentSize())
            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)),
                        ),
                    ),
            )
            Column(Modifier.align(Alignment.BottomStart).padding(12.dp)) {
                Text(
                    text = photo.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = photo.photographerName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                )
            }
        }
    }
}
