package ai.chaski.splashr.data.repository

import ai.chaski.splashr.data.model.Collection
import ai.chaski.splashr.data.model.Photo
import ai.chaski.splashr.data.model.Photographer
import ai.chaski.splashr.data.model.SearchFilter
import ai.chaski.splashr.data.model.Topic
import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth for photo data and user library state.
 *
 * This interface is the seam that keeps the app backend-agnostic: the current
 * implementation ([FakePhotoRepository]) serves a built-in sample catalogue, but
 * a real Unsplash/Pexels-backed implementation could be dropped in without any
 * UI changes.
 */
interface PhotoRepository {

    // ---- Catalogue ----------------------------------------------------------

    fun observeAllPhotos(): Flow<List<Photo>>
    fun observeTopics(): Flow<List<Topic>>
    fun observeFeaturedPhotos(): Flow<List<Photo>>
    fun observeTrendingPhotos(): Flow<List<Photo>>
    fun observePhotographers(): Flow<List<Photographer>>

    suspend fun getPhoto(id: String): Photo?
    suspend fun getPhotographer(id: String): Photographer?
    suspend fun getPhotosByPhotographer(photographerId: String): List<Photo>
    suspend fun search(filter: SearchFilter): List<Photo>

    // ---- Saved / offline ----------------------------------------------------

    fun observeSavedPhotos(): Flow<List<Photo>>
    fun observeSavedIds(): Flow<Set<String>>
    suspend fun toggleSaved(photo: Photo)

    // ---- Collections --------------------------------------------------------

    fun observeCollections(): Flow<List<Collection>>
    fun observeCollection(id: String): Flow<Collection?>
    suspend fun createCollection(name: String, description: String): String
    suspend fun renameCollection(id: String, name: String, description: String)
    suspend fun deleteCollection(id: String)
    suspend fun addPhotoToCollection(collectionId: String, photoId: String)
    suspend fun removePhotoFromCollection(collectionId: String, photoId: String)
}
