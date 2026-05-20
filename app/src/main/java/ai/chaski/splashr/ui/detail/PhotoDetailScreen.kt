package ai.chaski.splashr.ui.detail

import ai.chaski.splashr.ai.AiSuggestionService
import ai.chaski.splashr.data.model.AiTagSuggestion
import ai.chaski.splashr.data.model.Collection
import ai.chaski.splashr.data.model.Photo
import ai.chaski.splashr.data.model.PhotoCritique
import ai.chaski.splashr.data.model.Photographer
import ai.chaski.splashr.data.repository.PhotoRepository
import ai.chaski.splashr.ui.AppViewModelProvider
import ai.chaski.splashr.ui.collections.CreateCollectionDialog
import ai.chaski.splashr.ui.components.CircleImage
import ai.chaski.splashr.ui.components.LoadingState
import ai.chaski.splashr.ui.components.SplashrAsyncImage
import ai.chaski.splashr.ui.navigation.Routes
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.foundation.clickable
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PhotoDetailUiState(
    val photo: Photo? = null,
    val photographer: Photographer? = null,
    val collections: List<Collection> = emptyList(),
    val aiSuggestion: AiTagSuggestion? = null,
    val isLoadingAi: Boolean = false,
    val aiCritique: PhotoCritique? = null,
    val isLoadingCritique: Boolean = false,
)

/** All AI-card state for the detail screen — tag suggestions and the critique. */
private data class AiPanelState(
    val tagsLoading: Boolean = false,
    val tagSuggestion: AiTagSuggestion? = null,
    val critiqueLoading: Boolean = false,
    val critique: PhotoCritique? = null,
)

class PhotoDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: PhotoRepository,
    private val aiService: AiSuggestionService,
) : ViewModel() {

    private val photoId: String = checkNotNull(savedStateHandle[Routes.ARG_PHOTO_ID])

    private val basePhoto = MutableStateFlow<Photo?>(null)
    private val photographer = MutableStateFlow<Photographer?>(null)
    private val aiPanel = MutableStateFlow(AiPanelState())

    val uiState: StateFlow<PhotoDetailUiState> = combine(
        basePhoto,
        photographer,
        repository.observeCollections(),
        repository.observeSavedIds(),
        aiPanel,
    ) { photo, photographer, collections, savedIds, ai ->
        PhotoDetailUiState(
            photo = photo?.let { it.copy(isSaved = it.id in savedIds) },
            photographer = photographer,
            collections = collections,
            aiSuggestion = ai.tagSuggestion,
            isLoadingAi = ai.tagsLoading,
            aiCritique = ai.critique,
            isLoadingCritique = ai.critiqueLoading,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PhotoDetailUiState())

    init {
        viewModelScope.launch {
            val photo = repository.getPhoto(photoId)
            basePhoto.value = photo
            if (photo != null) {
                photographer.value = repository.getPhotographer(photo.photographerId)
            }
        }
    }

    fun toggleSave() {
        val photo = basePhoto.value ?: return
        viewModelScope.launch { repository.toggleSaved(photo) }
    }

    fun requestAiTags() {
        val photo = basePhoto.value ?: return
        viewModelScope.launch {
            aiPanel.update { it.copy(tagsLoading = true) }
            val suggestion = aiService.suggestTags(photo)
            aiPanel.update { it.copy(tagsLoading = false, tagSuggestion = suggestion) }
        }
    }

    fun requestCritique() {
        val photo = basePhoto.value ?: return
        viewModelScope.launch {
            aiPanel.update { it.copy(critiqueLoading = true) }
            val result = aiService.critique(photo)
            aiPanel.update { it.copy(critiqueLoading = false, critique = result) }
        }
    }

    fun addToCollection(collectionId: String) {
        viewModelScope.launch { repository.addPhotoToCollection(collectionId, photoId) }
    }

    fun removeFromCollection(collectionId: String) {
        viewModelScope.launch { repository.removePhotoFromCollection(collectionId, photoId) }
    }

    fun createCollectionAndAdd(name: String, description: String) {
        viewModelScope.launch {
            val id = repository.createCollection(name, description)
            repository.addPhotoToCollection(id, photoId)
        }
    }
}

@Composable
fun PhotoDetailScreen(
    onBack: () -> Unit,
    onPhotographerClick: (String) -> Unit,
    onTagClick: (String) -> Unit,
    viewModel: PhotoDetailViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showCollectionSheet by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.photo?.title ?: "Photo",
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
                    state.photo?.let { photo ->
                        IconButton(onClick = { sharePhoto(context, photo) }) {
                            Icon(Icons.Filled.Share, contentDescription = "Share photo")
                        }
                    }
                },
            )
        },
    ) { padding ->
        val photo = state.photo
        if (photo == null) {
            LoadingState(Modifier.padding(padding))
        } else {
            PhotoDetailContent(
                state = state,
                photo = photo,
                modifier = Modifier.padding(padding),
                onToggleSave = viewModel::toggleSave,
                onAddToCollection = { showCollectionSheet = true },
                onPhotographerClick = onPhotographerClick,
                onTagClick = onTagClick,
                onRequestAi = viewModel::requestAiTags,
                onRequestCritique = viewModel::requestCritique,
            )
        }
    }

    val photo = state.photo
    if (showCollectionSheet && photo != null) {
        AddToCollectionSheet(
            collections = state.collections,
            photoId = photo.id,
            onDismiss = { showCollectionSheet = false },
            onToggle = { collectionId, isMember ->
                if (isMember) viewModel.removeFromCollection(collectionId)
                else viewModel.addToCollection(collectionId)
            },
            onCreate = viewModel::createCollectionAndAdd,
        )
    }
}

@Composable
private fun PhotoDetailContent(
    state: PhotoDetailUiState,
    photo: Photo,
    modifier: Modifier,
    onToggleSave: () -> Unit,
    onAddToCollection: () -> Unit,
    onPhotographerClick: (String) -> Unit,
    onTagClick: (String) -> Unit,
    onRequestAi: () -> Unit,
    onRequestCritique: () -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        var showZoom by remember { mutableStateOf(false) }
        SplashrAsyncImage(
            url = photo.imageUrl,
            contentDescription = photo.title,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(photo.aspectRatio.coerceIn(0.7f, 1.6f))
                .clip(RoundedCornerShape(20.dp))
                .clickable { showZoom = true },
        )
        if (showZoom) {
            ZoomableImageDialog(
                url = photo.imageUrl,
                contentDescription = photo.title,
                onDismiss = { showZoom = false },
            )
        }

        Spacer(Modifier.height(16.dp))
        Text(photo.title, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(4.dp))
        if (photo.description.isNotBlank()) {
            Text(
                text = photo.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(16.dp))
        val photographer = state.photographer
        if (photographer != null) {
            Surface(
                onClick = { onPhotographerClick(photographer.id) },
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircleImage(photographer.profileImageUrl, photographer.name, 48.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(photographer.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "@${photographer.username} · View profile",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        } else if (photo.photographerName.isNotBlank()) {
            PexelsAttribution(photo)
        }

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilledTonalButton(onClick = onToggleSave, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = if (photo.isSaved) Icons.Filled.Favorite
                    else Icons.Outlined.FavoriteBorder,
                    contentDescription = null,
                )
                Spacer(Modifier.width(8.dp))
                Text(if (photo.isSaved) "Saved offline" else "Save")
            }
            OutlinedButton(onClick = onAddToCollection, modifier = Modifier.weight(1f)) {
                Icon(Icons.Outlined.BookmarkAdd, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Collection")
            }
        }

        Spacer(Modifier.height(20.dp))
        CritiqueCard(
            critique = state.aiCritique,
            isLoading = state.isLoadingCritique,
            onRequest = onRequestCritique,
        )

        Spacer(Modifier.height(20.dp))
        Text("Tags", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            photo.tags.forEach { tag ->
                AssistChip(onClick = { onTagClick(tag) }, label = { Text(tag) })
            }
        }

        Spacer(Modifier.height(20.dp))
        AiSuggestionCard(
            suggestion = state.aiSuggestion,
            isLoading = state.isLoadingAi,
            onRequest = onRequestAi,
            onTagClick = onTagClick,
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun AiSuggestionCard(
    suggestion: AiTagSuggestion?,
    isLoading: Boolean,
    onRequest: () -> Unit,
    onTagClick: (String) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        // Deep indigo so the white text on this card is genuinely readable
        // (AAA contrast). Visually distinct from the tertiaryContainer used by
        // the critique card above — both read as "AI" but differently coloured.
        color = Color(0xFF2D2A4A),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "AI tag suggestions",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )
            }
            Spacer(Modifier.height(8.dp))
            when {
                isLoading -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Analysing this photo…",
                        color = Color.White,
                    )
                }

                suggestion == null -> {
                    Text(
                        text = "Let Splashr suggest extra tags to make this photo easier to find.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onRequest) { Text("Suggest tags") }
                }

                else -> {
                    Text(
                        text = suggestion.explanation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                    )
                    Spacer(Modifier.height(10.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        suggestion.suggestedTags.forEach { tag ->
                            AssistChip(
                                onClick = { onTagClick(tag) },
                                label = { Text(tag) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = Color.White.copy(alpha = 0.14f),
                                    labelColor = Color.White,
                                ),
                                border = null,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddToCollectionSheet(
    collections: List<Collection>,
    photoId: String,
    onDismiss: () -> Unit,
    onToggle: (collectionId: String, isMember: Boolean) -> Unit,
    onCreate: (name: String, description: String) -> Unit,
) {
    var showCreate by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text("Add to collection", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            if (collections.isEmpty()) {
                Text(
                    text = "You don't have any collections yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                collections.forEach { collection ->
                    val isMember = photoId in collection.photoIds
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onToggle(collection.id, isMember) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = if (isMember) Icons.Filled.CheckCircle
                            else Icons.Outlined.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (isMember) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(collection.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "${collection.photoCount} photos",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = { showCreate = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("New collection")
            }
        }
    }

    if (showCreate) {
        CreateCollectionDialog(
            onDismiss = { showCreate = false },
            onConfirm = { name, description ->
                onCreate(name, description)
                showCreate = false
            },
        )
    }
}

/**
 * Full-screen image viewer with pinch-to-zoom and pan. Tapping the photo on the
 * detail screen opens this; the close button or a back gesture dismisses it.
 */
@Composable
private fun ZoomableImageDialog(
    url: String,
    contentDescription: String?,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        var scale by remember { mutableStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        val transformState = rememberTransformableState { zoomChange, panChange, _ ->
            scale = (scale * zoomChange).coerceIn(1f, 5f)
            offset = if (scale > 1f) offset + panChange else Offset.Zero
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f)),
            contentAlignment = Alignment.Center,
        ) {
            SplashrAsyncImage(
                url = url,
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }
                    .transformable(transformState),
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
            }
        }
    }
}

/**
 * AI photography critique card — composition, light, colour, and a closing
 * takeaway. Tapping "Analyse" calls Claude (vision); the result is cached so
 * subsequent visits to the same photo display instantly.
 */
@Composable
private fun CritiqueCard(
    critique: PhotoCritique?,
    isLoading: Boolean,
    onRequest: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Why this shot works",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
            Spacer(Modifier.height(8.dp))
            when {
                isLoading -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Analysing the photograph…",
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }

                critique == null -> {
                    Text(
                        text = "Get a quick photography critique — composition, light, " +
                            "colour, and what makes this shot work.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onRequest) { Text("Analyse this photo") }
                }

                else -> {
                    CritiqueSection("Composition", critique.composition)
                    Spacer(Modifier.height(8.dp))
                    CritiqueSection("Light", critique.light)
                    Spacer(Modifier.height(8.dp))
                    CritiqueSection("Colour", critique.color)
                    if (critique.takeaway.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = critique.takeaway,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            fontStyle = FontStyle.Italic,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CritiqueSection(label: String, body: String) {
    if (body.isBlank()) return
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
    }
}

/**
 * Photographer credit for a photo with no in-app profile (i.e. sourced from
 * Pexels). Tapping it opens the photographer's Pexels page — Pexels requires
 * this attribution link for any displayed photo.
 */
@Composable
private fun PexelsAttribution(photo: Photo) {
    val context = LocalContext.current
    val link = photo.photographerUrl.ifBlank { "https://www.pexels.com" }
    Surface(
        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link))) },
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(photo.photographerName, style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Photo provided by Pexels · Tap to view profile ↗",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Opens the system share sheet with the photo's title, author, and image URL. */
private fun sharePhoto(context: Context, photo: Photo) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, photo.title)
        putExtra(
            Intent.EXTRA_TEXT,
            "${photo.title} by ${photo.photographerName}\n${photo.imageUrl}",
        )
    }
    context.startActivity(Intent.createChooser(intent, "Share photo"))
}
