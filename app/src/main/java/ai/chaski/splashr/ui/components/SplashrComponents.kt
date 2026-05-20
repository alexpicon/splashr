package ai.chaski.splashr.ui.components

import ai.chaski.splashr.data.model.Photo
import ai.chaski.splashr.data.model.Topic
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest

/** Red used for the "saved" heart — a favourite, not a theme accent. */
private val SavedHeartRed = Color(0xFFFF4458)

/**
 * Network image with a surface-coloured placeholder background and crossfade.
 *
 * While the image is loading a spinner is shown; if it fails (e.g. the device is
 * offline) a broken-image icon is shown instead of a silent blank box.
 */
@Composable
fun SplashrAsyncImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    var state by remember(url) {
        mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty)
    }
    Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(url)
                .crossfade(true)
                .build(),
            contentDescription = contentDescription,
            contentScale = contentScale,
            onState = { state = it },
            modifier = Modifier.matchParentSize(),
        )
        when (state) {
            is AsyncImagePainter.State.Loading -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center).size(28.dp),
                strokeWidth = 2.dp,
            )

            is AsyncImagePainter.State.Error -> Icon(
                imageVector = Icons.Outlined.BrokenImage,
                contentDescription = "Image failed to load",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center).size(32.dp),
            )

            else -> Unit
        }
    }
}

/** A circular avatar image. */
@Composable
fun CircleImage(
    url: String,
    contentDescription: String?,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    SplashrAsyncImage(
        url = url,
        contentDescription = contentDescription,
        modifier = modifier.size(size).clip(CircleShape),
    )
}

/**
 * Small circular button drawn over an image (save / remove actions).
 *
 * The outer touch area is 48dp (Material minimum); the visible chip is 28dp
 * and pinned to the top-end of that area, so the button still appears in the
 * photo's corner while the tappable region extends inward.
 */
@Composable
private fun OverlayIconButton(
    icon: ImageVector,
    tint: Color,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .size(48.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.TopEnd,
    ) {
        Box(
            Modifier
                .size(28.dp)
                .background(Color.Black.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(16.dp))
        }
    }
}

/**
 * A single photo cell: image, title gradient, and optional save/remove overlays.
 */
@Composable
fun PhotoCard(
    photo: Photo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onToggleSave: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 2.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Box {
            SplashrAsyncImage(
                url = photo.imageUrl,
                contentDescription = photo.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(photo.aspectRatio.coerceIn(0.62f, 1.7f)),
            )
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)),
                        ),
                    )
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
                Text(
                    text = photo.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (onToggleSave != null) {
                OverlayIconButton(
                    icon = if (photo.isSaved) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    tint = if (photo.isSaved) SavedHeartRed else Color.White,
                    contentDescription = if (photo.isSaved) "Remove from saved" else "Save photo",
                    onClick = onToggleSave,
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                )
            }
            if (onRemove != null) {
                OverlayIconButton(
                    icon = Icons.Filled.Close,
                    tint = Color.White,
                    contentDescription = "Remove from collection",
                    onClick = onRemove,
                    modifier = Modifier.align(Alignment.TopStart).padding(6.dp),
                )
            }
        }
    }
}

/**
 * A two-column masonry grid of photos, with an optional full-width header that
 * scrolls with the content.
 */
@Composable
fun PhotoStaggeredGrid(
    photos: List<Photo>,
    onPhotoClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onToggleSave: ((Photo) -> Unit)? = null,
    onRemove: ((Photo) -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(14.dp),
    header: (@Composable () -> Unit)? = null,
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalItemSpacing = 12.dp,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (header != null) {
            item(span = StaggeredGridItemSpan.FullLine) { header() }
        }
        items(photos, key = { it.id }) { photo ->
            PhotoCard(
                photo = photo,
                onClick = { onPhotoClick(photo.id) },
                onToggleSave = onToggleSave?.let { callback -> { callback(photo) } },
                onRemove = onRemove?.let { callback -> { callback(photo) } },
            )
        }
    }
}

/** A section title with an optional subtitle and trailing action slot. */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        action?.invoke()
    }
}

/** A compact topic card for the Home trending rail. */
@Composable
fun TopicCard(
    topic: Topic,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.size(width = 158.dp, height = 104.dp),
    ) {
        Box {
            SplashrAsyncImage(topic.coverImageUrl, topic.title, Modifier.matchParentSize())
            Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.34f)))
            Column(Modifier.align(Alignment.BottomStart).padding(10.dp)) {
                Text(
                    text = topic.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )
                Text(
                    text = "${topic.photoCount} photos",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                )
            }
        }
    }
}

/** Centered empty-state placeholder. */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(56.dp),
        )
        Spacer(Modifier.height(14.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** Centered loading spinner. */
@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
