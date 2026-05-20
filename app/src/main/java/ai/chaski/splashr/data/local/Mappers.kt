package ai.chaski.splashr.data.local

import ai.chaski.splashr.data.model.Photo

/** Conversions between Room entities and domain models. */

private const val TAG_SEPARATOR = "|"

fun SavedPhotoEntity.toPhoto(): Photo = Photo(
    id = id,
    title = title,
    description = description,
    imageUrl = imageUrl,
    photographerId = photographerId,
    photographerName = photographerName,
    tags = if (tags.isBlank()) emptyList() else tags.split(TAG_SEPARATOR),
    topic = topic,
    width = width,
    height = height,
    dominantColor = dominantColor,
    isSaved = true,
)

fun Photo.toSavedEntity(savedAt: Long = System.currentTimeMillis()): SavedPhotoEntity = SavedPhotoEntity(
    id = id,
    title = title,
    description = description,
    imageUrl = imageUrl,
    photographerId = photographerId,
    photographerName = photographerName,
    tags = tags.joinToString(TAG_SEPARATOR),
    topic = topic,
    width = width,
    height = height,
    dominantColor = dominantColor,
    savedAt = savedAt,
)
