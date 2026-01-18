package space.suiten.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import space.suiten.data.local.dao.*
import space.suiten.data.local.entity.*

/**
 * Main Room database for Shiorio app
 *
 * CRITICAL PRIVACY REQUIREMENT:
 * OCR text must NEVER be stored in this database or any table.
 * OCR text should only exist in memory during AI processing,
 * and must be immediately discarded after AI response is received.
 */
@Database(
    entities = [
        BookEntity::class,
        ReadingSessionEntity::class,
        CaptureEntity::class,
        AIResultEntity::class,
        NextActionEntity::class,
        FeedbackLogEntity::class,
        ActionLogEntity::class,
        UserProfileEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class SuiteNDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun readingSessionDao(): ReadingSessionDao
    abstract fun captureDao(): CaptureDao
    abstract fun aiResultDao(): AIResultDao
    abstract fun nextActionDao(): NextActionDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun feedbackLogDao(): FeedbackLogDao

    companion object {
        @Volatile
        private var INSTANCE: SuiteNDatabase? = null

        fun getDatabase(context: Context): SuiteNDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SuiteNDatabase::class.java,
                    "suiten.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
