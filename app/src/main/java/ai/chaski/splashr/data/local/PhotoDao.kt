package ai.chaski.splashr.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {

    @Query("SELECT * FROM saved_photos ORDER BY savedAt DESC")
    fun observeSavedPhotos(): Flow<List<SavedPhotoEntity>>

    @Query("SELECT id FROM saved_photos")
    fun observeSavedIds(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM saved_photos WHERE id = :id)")
    suspend fun isSaved(id: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(photo: SavedPhotoEntity)

    @Query("DELETE FROM saved_photos WHERE id = :id")
    suspend fun delete(id: String)
}
