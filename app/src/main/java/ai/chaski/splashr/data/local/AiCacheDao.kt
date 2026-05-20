package ai.chaski.splashr.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/** Stores Claude-generated AI artefacts (tag suggestions, critiques) so each photo is analysed only once. */
@Dao
interface AiCacheDao {

    @Query("SELECT * FROM ai_suggestions WHERE photoId = :photoId")
    suspend fun getSuggestion(photoId: String): AiSuggestionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSuggestion(suggestion: AiSuggestionEntity)

    @Query("SELECT * FROM ai_critiques WHERE photoId = :photoId")
    suspend fun getCritique(photoId: String): AiCritiqueEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCritique(critique: AiCritiqueEntity)
}
