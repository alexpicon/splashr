package ai.chaski.splashr.ui.collections

import ai.chaski.splashr.data.model.Collection
import ai.chaski.splashr.data.model.Photo
import ai.chaski.splashr.data.repository.PhotoRepository
import ai.chaski.splashr.ui.AppViewModelProvider
import ai.chaski.splashr.ui.components.EmptyState
import ai.chaski.splashr.ui.components.LoadingState
import ai.chaski.splashr.ui.components.PhotoStaggeredGrid
import ai.chaski.splashr.ui.components.SplashrAsyncImage
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

/** A collection paired with the cover image of its first photo. */
data class CollectionCard(val collection: Collection, val coverUrl: String?)

class LibraryViewModel(private val repository: PhotoRepository) : ViewModel() {

    val savedPhotos: StateFlow<List<Photo>> = repository.observeSavedPhotos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val collections: StateFlow<List<CollectionCard>> = combine(
        repository.observeCollections(),
        repository.observeAllPhotos(),
    ) { collections, allPhotos ->
        collections.map { collection ->
            CollectionCard(
                collection = collection,
                coverUrl = allPhotos.firstOrNull { it.id in collection.photoIds }?.imageUrl,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun createCollection(name: String, description: String) {
        viewModelScope.launch { repository.createCollection(name, description) }
    }

    fun deleteCollection(id: String) {
        viewModelScope.launch { repository.deleteCollection(id) }
    }

    fun toggleSave(photo: Photo) {
        viewModelScope.launch { repository.toggleSaved(photo) }
    }
}

@Composable
fun LibraryScreen(
    onPhotoClick: (String) -> Unit,
    onCollectionClick: (String) -> Unit,
    viewModel: LibraryViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val savedPhotos by viewModel.savedPhotos.collectAsStateWithLifecycle()
    val collections by viewModel.collections.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showCreate by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Library") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (selectedTab == 1) {
                FloatingActionButton(onClick = { showCreate = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "New collection")
                }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Saved (${savedPhotos.size})") },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Collections (${collections.size})") },
                )
            }
            Box(Modifier.weight(1f)) {
                if (selectedTab == 0) {
                    if (savedPhotos.isEmpty()) {
                        EmptyState(
                            icon = Icons.Outlined.FavoriteBorder,
                            title = "No saved photos",
                            message = "Tap the heart on any photo to save it for offline viewing.",
                        )
                    } else {
                        PhotoStaggeredGrid(
                            photos = savedPhotos,
                            onPhotoClick = onPhotoClick,
                            // Tapping the heart in the Library always unsaves;
                            // surface an UNDO so accidental taps don't lose data.
                            onToggleSave = { photo ->
                                viewModel.toggleSave(photo)
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = "Removed from saved",
                                        actionLabel = "Undo",
                                        duration = SnackbarDuration.Short,
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.toggleSave(photo)
                                    }
                                }
                            },
                        )
                    }
                } else {
                    if (collections.isEmpty()) {
                        EmptyState(
                            icon = Icons.Outlined.CollectionsBookmark,
                            title = "No collections yet",
                            message = "Create a collection to group the photos you love.",
                        )
                    } else {
                        CollectionList(
                            collections = collections,
                            onCollectionClick = onCollectionClick,
                            onDelete = viewModel::deleteCollection,
                        )
                    }
                }
            }
        }
    }

    if (showCreate) {
        CreateCollectionDialog(
            onDismiss = { showCreate = false },
            onConfirm = { name, description ->
                viewModel.createCollection(name, description)
                showCreate = false
            },
        )
    }
}

@Composable
private fun CollectionList(
    collections: List<CollectionCard>,
    onCollectionClick: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize()) {
        items(collections, key = { it.collection.id }) { card ->
            CollectionRow(
                card = card,
                onClick = { onCollectionClick(card.collection.id) },
                onDelete = { onDelete(card.collection.id) },
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun CollectionRow(
    card: CollectionCard,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (card.coverUrl != null) {
                SplashrAsyncImage(
                    url = card.coverUrl,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)),
                )
            } else {
                Box(
                    Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.PhotoLibrary, contentDescription = null)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(card.collection.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${card.collection.photoCount} photos",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (card.collection.description.isNotBlank()) {
                    Text(
                        text = card.collection.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Delete collection") },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }
}

/**
 * Dialog for creating or renaming a collection. Reused by the Library and
 * Photo Detail screens.
 */
@Composable
fun CreateCollectionDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String) -> Unit,
    title: String = "New collection",
    confirmLabel: String = "Create",
    initialName: String = "",
    initialDescription: String = "",
) {
    var name by remember { mutableStateOf(initialName) }
    var description by remember { mutableStateOf(initialDescription) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim(), description.trim()) },
                enabled = name.isNotBlank(),
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
