package ai.chaski.splashr.data.repository

import ai.chaski.splashr.data.local.CollectionDao
import ai.chaski.splashr.data.local.CollectionEntity
import ai.chaski.splashr.data.local.CollectionPhotoCrossRef
import ai.chaski.splashr.data.local.PhotoDao
import ai.chaski.splashr.data.local.toPhoto
import ai.chaski.splashr.data.local.toSavedEntity
import ai.chaski.splashr.data.model.Collection
import ai.chaski.splashr.data.model.Orientation
import ai.chaski.splashr.data.model.Photo
import ai.chaski.splashr.data.model.Photographer
import ai.chaski.splashr.data.model.SearchFilter
import ai.chaski.splashr.data.model.Topic
import ai.chaski.splashr.data.sample.SampleData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Repository backed by the built-in [SampleData] catalogue for photos, and the
 * Room database for user library state (saved photos + collections).
 *
 * Saved state is folded into every emitted [Photo] so the UI's "saved" indicator
 * stays correct everywhere without each screen having to reconcile it.
 */
class FakePhotoRepository(
    private val photoDao: PhotoDao,
    private val collectionDao: CollectionDao,
) : PhotoRepository {

    private val catalogue: List<Photo> = SampleData.photos

    // ---- Catalogue ----------------------------------------------------------

    override fun observeSavedIds(): Flow<Set<String>> =
        photoDao.observeSavedIds().map { it.toSet() }

    private fun List<Photo>.withSavedState(savedIds: Set<String>): List<Photo> =
        map { it.copy(isSaved = it.id in savedIds) }

    override fun observeAllPhotos(): Flow<List<Photo>> =
        observeSavedIds().map { catalogue.withSavedState(it) }

    override fun observeTopics(): Flow<List<Topic>> = flowOf(SampleData.topics)

    override fun observePhotographers(): Flow<List<Photographer>> =
        flowOf(SampleData.photographers)

    override fun observeFeaturedPhotos(): Flow<List<Photo>> =
        observeSavedIds().map { SampleData.featuredPhotos.withSavedState(it) }

    override fun observeTrendingPhotos(): Flow<List<Photo>> =
        observeSavedIds().map { SampleData.trendingPhotos.withSavedState(it) }

    override suspend fun getPhoto(id: String): Photo? {
        val savedIds = observeSavedIds().first()
        return catalogue.find { it.id == id }?.copy(isSaved = id in savedIds)
    }

    override suspend fun getPhotographer(id: String): Photographer? =
        SampleData.photographers.find { it.id == id }

    override suspend fun getPhotosByPhotographer(photographerId: String): List<Photo> {
        val savedIds = observeSavedIds().first()
        return catalogue.filter { it.photographerId == photographerId }.withSavedState(savedIds)
    }

    override suspend fun search(filter: SearchFilter): List<Photo> {
        val savedIds = observeSavedIds().first()
        val queryTokens = filter.query.lowercase().split(" ").filter { it.isNotBlank() }
        return catalogue.filter { it.matches(filter, queryTokens) }.withSavedState(savedIds)
    }

    private fun Photo.matches(filter: SearchFilter, queryTokens: List<String>): Boolean {
        if (queryTokens.isNotEmpty()) {
            val haystack = (listOf(title, description, topic, photographerName) + tags)
                .joinToString(" ")
                .lowercase()
            if (queryTokens.none { haystack.contains(it) }) return false
        }
        filter.topic?.let { if (!topic.equals(it, ignoreCase = true)) return false }
        if (filter.orientation != Orientation.ANY && orientation != filter.orientation) return false
        filter.color?.let { if (!dominantColor.equals(it, ignoreCase = true)) return false }
        filter.photographer?.let {
            if (!photographerName.contains(it, ignoreCase = true)) return false
        }
        if (filter.tags.isNotEmpty() &&
            filter.tags.none { wanted -> tags.any { it.contains(wanted, ignoreCase = true) } }
        ) {
            return false
        }
        return true
    }

    // ---- Saved / offline ----------------------------------------------------

    override fun observeSavedPhotos(): Flow<List<Photo>> =
        photoDao.observeSavedPhotos().map { saved -> saved.map { it.toPhoto() } }

    override suspend fun toggleSaved(photo: Photo) {
        if (photoDao.isSaved(photo.id)) {
            photoDao.delete(photo.id)
        } else {
            photoDao.insert(photo.toSavedEntity())
        }
    }

    // ---- Collections --------------------------------------------------------

    override fun observeCollections(): Flow<List<Collection>> =
        combine(
            collectionDao.observeCollections(),
            collectionDao.observeAllCrossRefs(),
        ) { collections, crossRefs ->
            collections.map { entity ->
                Collection(
                    id = entity.id,
                    name = entity.name,
                    description = entity.description,
                    photoIds = crossRefs
                        .filter { it.collectionId == entity.id }
                        .sortedBy { it.addedAt }
                        .map { it.photoId },
                    createdAt = entity.createdAt,
                )
            }
        }

    override fun observeCollection(id: String): Flow<Collection?> =
        observeCollections().map { collections -> collections.find { it.id == id } }

    override suspend fun createCollection(name: String, description: String): String {
        val id = "col-${System.currentTimeMillis()}"
        collectionDao.insertCollection(
            CollectionEntity(id, name, description, System.currentTimeMillis()),
        )
        return id
    }

    override suspend fun renameCollection(id: String, name: String, description: String) {
        val existing = collectionDao.observeCollections().first().find { it.id == id } ?: return
        collectionDao.updateCollection(CollectionEntity(id, name, description, existing.createdAt))
    }

    override suspend fun deleteCollection(id: String) {
        collectionDao.deleteCrossRefsForCollection(id)
        collectionDao.deleteCollection(id)
    }

    override suspend fun addPhotoToCollection(collectionId: String, photoId: String) {
        collectionDao.addPhotoToCollection(
            CollectionPhotoCrossRef(collectionId, photoId, System.currentTimeMillis()),
        )
    }

    override suspend fun removePhotoFromCollection(collectionId: String, photoId: String) {
        collectionDao.removePhotoFromCollection(collectionId, photoId)
    }
}
