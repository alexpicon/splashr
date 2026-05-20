package ai.chaski.splashr.ui.collections

import ai.chaski.splashr.data.model.Collection
import ai.chaski.splashr.data.model.Photo
import ai.chaski.splashr.data.repository.PhotoRepository
import ai.chaski.splashr.ui.AppViewModelProvider
import ai.chaski.splashr.ui.components.EmptyState
import ai.chaski.splashr.ui.components.LoadingState
import ai.chaski.splashr.ui.components.PhotoStaggeredGrid
import ai.chaski.splashr.ui.navigation.Routes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CollectionDetailUiState(
    val collection: Collection? = null,
    val photos: List<Photo> = emptyList(),
    val isLoading: Boolean = true,
)

class CollectionDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: PhotoRepository,
) : ViewModel() {

    private val collectionId: String = checkNotNull(savedStateHandle[Routes.ARG_COLLECTION_ID])

    val uiState: StateFlow<CollectionDetailUiState> = combine(
        repository.observeCollection(collectionId),
        repository.observeAllPhotos(),
    ) { collection, allPhotos ->
        val photos = collection?.photoIds
            ?.mapNotNull { id -> allPhotos.find { it.id == id } }
            ?: emptyList()
        CollectionDetailUiState(collection, photos, isLoading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CollectionDetailUiState())

    fun removePhoto(photoId: String) {
        viewModelScope.launch { repository.removePhotoFromCollection(collectionId, photoId) }
    }

    fun rename(name: String, description: String) {
        viewModelScope.launch { repository.renameCollection(collectionId, name, description) }
    }

    fun deleteCollection() {
        viewModelScope.launch { repository.deleteCollection(collectionId) }
    }

    fun toggleSave(photo: Photo) {
        viewModelScope.launch { repository.toggleSaved(photo) }
    }
}

@Composable
fun CollectionDetailScreen(
    onBack: () -> Unit,
    onPhotoClick: (String) -> Unit,
    viewModel: CollectionDetailViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var menuOpen by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    val collection = state.collection

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = collection?.name ?: "Collection",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (collection != null) {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Collection options")
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Rename") },
                                onClick = {
                                    menuOpen = false
                                    showRename = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Delete collection") },
                                onClick = {
                                    menuOpen = false
                                    showDelete = true
                                },
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading -> LoadingState(Modifier.padding(padding))
            state.photos.isEmpty() -> EmptyState(
                icon = Icons.Outlined.PhotoLibrary,
                title = "This collection is empty",
                message = "Open any photo and use \"Collection\" to add it here.",
                modifier = Modifier.padding(padding),
            )
            else -> PhotoStaggeredGrid(
                photos = state.photos,
                onPhotoClick = onPhotoClick,
                onToggleSave = viewModel::toggleSave,
                onRemove = { photo -> viewModel.removePhoto(photo.id) },
                modifier = Modifier.padding(padding),
                header = if (collection?.description?.isNotBlank() == true) {
                    {
                        Column(Modifier.fillMaxWidth()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = collection.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                } else {
                    null
                },
            )
        }
    }

    if (showRename && collection != null) {
        CreateCollectionDialog(
            onDismiss = { showRename = false },
            onConfirm = { name, description ->
                viewModel.rename(name, description)
                showRename = false
            },
            title = "Rename collection",
            confirmLabel = "Save",
            initialName = collection.name,
            initialDescription = collection.description,
        )
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Delete collection?") },
            text = { Text("This removes the collection. Your saved photos are not affected.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDelete = false
                        viewModel.deleteCollection()
                        onBack()
                    },
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) { Text("Cancel") }
            },
        )
    }
}
