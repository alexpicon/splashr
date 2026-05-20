package ai.chaski.splashr.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/** The app's local Room database — stores offline-saved photos, collections, and AI cache. */
@Database(
    entities = [
        SavedPhotoEntity::class,
        CollectionEntity::class,
        CollectionPhotoCrossRef::class,
        AiSuggestionEntity::class,
        AiCritiqueEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class SplashrDatabase : RoomDatabase() {

    abstract fun photoDao(): PhotoDao
    abstract fun collectionDao(): CollectionDao
    abstract fun aiCacheDao(): AiCacheDao

    companion object {
        @Volatile
        private var instance: SplashrDatabase? = null

        fun get(context: Context): SplashrDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    SplashrDatabase::class.java,
                    "splashr.db",
                )
                    // The AI cache (schema v2) is disposable; on a schema change
                    // just rebuild the local DB rather than ship a migration.
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
    }
}
