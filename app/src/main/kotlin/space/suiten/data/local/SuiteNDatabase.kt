package space.suiten.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 4,
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

        /** Adds book-level summary metrics for the North Star Metric. */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                listOf(
                    "summaryGeneratedAt INTEGER",
                    "summarySavedAt INTEGER",
                    "summaryEditedAt INTEGER",
                    "summaryHelpfulness INTEGER",
                    "summaryCaptureCount INTEGER",
                    "summaryCompletedAt INTEGER"
                ).forEach { column ->
                    db.execSQL("ALTER TABLE books ADD COLUMN $column")
                }
            }
        }

        fun getDatabase(context: Context): SuiteNDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SuiteNDatabase::class.java,
                    "suiten.db"
                )
                    .addMigrations(MIGRATION_3_4)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
