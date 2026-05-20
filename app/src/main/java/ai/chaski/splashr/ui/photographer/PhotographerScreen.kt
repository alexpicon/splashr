package ai.chaski.splashr.ui.photographer

import ai.chaski.splashr.data.model.Photo
import ai.chaski.splashr.data.model.Photographer
import ai.chaski.splashr.data.repository.PhotoRepository
import ai.chaski.splashr.ui.AppViewModelProvider
import ai.chaski.splashr.ui.components.CircleImage
import ai.chaski.splashr.ui.components.EmptyState
import ai.chaski.splashr.ui.components.LoadingState
import ai.chaski.splashr.ui.components.PhotoStaggeredGrid
import ai.chaski.splashr.ui.components.SectionHeader
import ai.chaski.splashr.ui.navigation.Routes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PhotographerUiState(
    val photographer: Photographer? = null,
    val photos: List<Photo> = emptyList(),
    val isLoading: Boolean = true,
)

class PhotographerViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: PhotoRepository,
) : ViewModel() {

    private val photographerId: String =
        checkNotNull(savedStateHandle[Routes.ARG_PHOTOGRAPHER_ID])

    private val photographer = MutableStateFlow<Photographer?>(null)
    private val photos = MutableStateFlow<List<Photo>>(emptyList())
    private val loaded = MutableStateFlow(false)

    val uiState: StateFlow<PhotographerUiState> = combine(
        photographer,
        photos,
        repository.observeSavedIds(),
        loaded,
    ) { photographer, photos, savedIds, loaded ->
        PhotographerUiState(
            photographer = photographer,
            photos = photos.map { it.copy(isSaved = it.id in savedIds) },
            isLoading = !loaded,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PhotographerUiState())

    init {
        viewModelScope.launch {
            photographer.value = repository.getPhotographer(photographerId)
            photos.value = repository.getPhotosByPhotographer(photographerId)
            loaded.value = true
        }
    }

    fun toggleSave(photo: Photo) {
        viewModelScope.launch { repository.toggleSaved(photo) }
    }
}

@Composable
fun PhotographerScreen(
    onBack: () -> Unit,
    onPhotoClick: (String) -> Unit,
    viewModel: PhotographerViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.photographer?.name ?: "Photographer") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        val photographer = state.photographer
        when {
            state.isLoading -> LoadingState(Modifier.padding(padding))
            photographer == null -> EmptyState(
                icon = Icons.Outlined.Person,
                title = "Photographer not found",
                message = "This profile could not be loaded.",
                modifier = Modifier.padding(padding),
            )
            else -> PhotoStaggeredGrid(
                photos = state.photos,
                onPhotoClick = onPhotoClick,
                onToggleSave = viewModel::toggleSave,
                modifier = Modifier.padding(padding),
                header = { PhotographerHeader(photographer, state.photos.size) },
            )
        }
    }
}

@Composable
private fun PhotographerHeader(photographer: Photographer, shownCount: Int) {
    Column(Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircleImage(photographer.profileImageUrl, photographer.name, 80.dp)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(photographer.name, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "@${photographer.username}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = photographer.location,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(photographer.bio, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "${photographer.totalPhotos} photos in portfolio",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(18.dp))
        SectionHeader("In this app", subtitle = "$shownCount photos by ${photographer.name}")
        Spacer(Modifier.height(10.dp))
    }
}
