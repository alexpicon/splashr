package ai.chaski.splashr.data.repository

import ai.chaski.splashr.data.model.Orientation
import ai.chaski.splashr.data.model.Photo
import ai.chaski.splashr.data.model.SearchFilter
import ai.chaski.splashr.data.remote.PexelsApi
import ai.chaski.splashr.data.remote.isPexelsId
import ai.chaski.splashr.data.remote.pexelsNumericId
import ai.chaski.splashr.data.remote.toPhoto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Repository that serves the photo *catalogue* from the Pexels API, while
 * delegating everything else — topics, photographer profiles, saved photos and
 * collections — to [library] via Kotlin interface delegation (`by library`).
 *
 * This is the "hybrid" setup: real Pexels photos in the grids and search, but
 * the curated topic descriptions and photographer profiles from SampleData are
 * kept intact. Used only when a Pexels API key is configured (see AppContainer);
 * with no key the app falls back to [library] directly.
 */
class PexelsPhotoRepository(
    private val api: PexelsApi,
    private val apiKey: String,
    private val library: PhotoRepository,
) : PhotoRepository by library {

    // Curated photos back all three Home rails; fetch them once and reuse.
    private val curatedMutex = Mutex()
    private var curatedCache: List<Photo>? = null

    private suspend fun curated(): List<Photo> {
        curatedCache?.let { return it }
        return curatedMutex.withLock {
            curatedCache ?: runCatching {
                api.curated(apiKey).photos.map { it.toPhoto() }
            }.getOrDefault(emptyList()).also { curatedCache = it }
        }
    }

    private suspend fun List<Photo>.withSavedState(): List<Photo> {
        val savedIds = library.observeSavedIds().first()
        return map { it.copy(isSaved = it.id in savedIds) }
    }

    // ---- Catalogue (Pexels-backed) -----------------------------------------

    override fun observeAllPhotos(): Flow<List<Photo>> =
        library.observeSavedIds().map { savedIds ->
            curated().map { it.copy(isSaved = it.id in savedIds) }
        }

    override fun observeFeaturedPhotos(): Flow<List<Photo>> =
        library.observeSavedIds().map { savedIds ->
            curated().take(FEATURED_COUNT).map { it.copy(isSaved = it.id in savedIds) }
        }

    override fun observeTrendingPhotos(): Flow<List<Photo>> =
        library.observeSavedIds().map { savedIds ->
            curated().drop(FEATURED_COUNT).map { it.copy(isSaved = it.id in savedIds) }
        }

    override suspend fun getPhoto(id: String): Photo? {
        if (!isPexelsId(id)) return library.getPhoto(id)
        val numericId = pexelsNumericId(id) ?: return null
        val photo = runCatching { api.photo(apiKey, numericId).toPhoto() }.getOrNull()
            ?: return null
        val savedIds = library.observeSavedIds().first()
        return photo.copy(isSaved = photo.id in savedIds)
    }

    override suspend fun getPhotosByPhotographer(photographerId: String): List<Photo> =
        // Pexels has no "photos by photographer" endpoint; in-app profiles are
        // SampleData only, so this only ever runs for a SampleData photographer.
        if (isPexelsId(photographerId)) emptyList()
        else library.getPhotosByPhotographer(photographerId)

    override suspend fun search(filter: SearchFilter): List<Photo> {
        val query = filter.query.ifBlank { filter.topic ?: filter.photographer ?: "photography" }
        val orientation = when (filter.orientation) {
            Orientation.LANDSCAPE -> "landscape"
            Orientation.PORTRAIT -> "portrait"
            Orientation.SQUARE -> "square"
            Orientation.ANY -> null
        }
        return runCatching {
            api.search(
                apiKey = apiKey,
                query = query,
                orientation = orientation,
                color = toPexelsColor(filter.color),
            ).photos.map { it.toPhoto(topic = filter.topic.orEmpty()) }
        }.getOrDefault(emptyList()).withSavedState()
    }

    /**
     * Maps the app's colour labels onto Pexels' supported palette
     * (https://www.pexels.com/api/documentation/#photos-search). Returns `null`
     * for labels Pexels doesn't recognise — the API ignores the filter rather
     * than returning unexpected results.
     */
    private fun toPexelsColor(color: String?): String? = when (color?.lowercase()) {
        "blue" -> "blue"
        "green" -> "green"
        "red" -> "red"
        "orange", "warm" -> "orange"
        "teal" -> "turquoise"
        "white" -> "white"
        "dark", "monochrome" -> "black"
        else -> null
    }

    private companion object {
        /** How many curated photos go to the Home "Featured" rail. */
        const val FEATURED_COUNT = 6
    }
}
