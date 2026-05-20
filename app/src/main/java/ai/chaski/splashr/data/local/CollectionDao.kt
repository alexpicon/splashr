package ai.chaski.splashr.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDao {

    @Query("SELECT * FROM collections ORDER BY createdAt DESC")
    fun observeCollections(): Flow<List<CollectionEntity>>

    @Query("SELECT * FROM collection_photos")
    fun observeAllCrossRefs(): Flow<List<CollectionPhotoCrossRef>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollection(collection: CollectionEntity)

    @Update
    suspend fun updateCollection(collection: CollectionEntity)

    @Query("DELETE FROM collections WHERE id = :id")
    suspend fun deleteCollection(id: String)

    @Query("DELETE FROM collection_photos WHERE collectionId = :id")
    suspend fun deleteCrossRefsForCollection(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addPhotoToCollection(ref: CollectionPhotoCrossRef)

    @Query("DELETE FROM collection_photos WHERE collectionId = :collectionId AND photoId = :photoId")
    suspend fun removePhotoFromCollection(collectionId: String, photoId: String)
}
