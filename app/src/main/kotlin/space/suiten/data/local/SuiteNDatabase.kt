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
    version = 5,
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

        private val SUMMARY_COLUMNS = listOf(
            "summaryGeneratedAt", "summarySavedAt", "summaryEditedAt",
            "summaryHelpfulness", "summaryCaptureCount", "summaryCompletedAt"
        )

        /**
         * Adds book-level summary metrics for the North Star Metric.
         * Skips columns that already exist, so it is safe on any earlier schema.
         */
        private fun addSummaryColumns(db: SupportSQLiteDatabase) {
            val existing = columnsOf(db, "books")
            SUMMARY_COLUMNS.filterNot { it in existing }.forEach { column ->
                db.execSQL("ALTER TABLE books ADD COLUMN $column INTEGER")
            }
        }

        private fun columnsOf(db: SupportSQLiteDatabase, table: String): Set<String> {
            val columns = mutableSetOf<String>()
            db.query("PRAGMA table_info($table)").use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                while (cursor.moveToNext()) columns += cursor.getString(nameIndex)
            }
            return columns
        }

        /**
         * Some development builds (installed as version 4) added next_actions.createdAt,
         * which the entity doesn't have. Rebuild the table without it; older SQLite
         * versions can't drop columns.
         */
        private fun dropNextActionsCreatedAt(db: SupportSQLiteDatabase) {
            if ("createdAt" !in columnsOf(db, "next_actions")) return
            db.execSQL("CREATE TABLE `next_actions_new` (`id` TEXT NOT NULL, `aiResultId` TEXT NOT NULL, `type` TEXT NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `query` TEXT, `link` TEXT, `rank` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`aiResultId`) REFERENCES `ai_results`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            db.execSQL("INSERT INTO `next_actions_new` (`id`, `aiResultId`, `type`, `title`, `description`, `query`, `link`, `rank`) SELECT `id`, `aiResultId`, `type`, `title`, `description`, `query`, `link`, `rank` FROM `next_actions`")
            db.execSQL("DROP TABLE `next_actions`")
            db.execSQL("ALTER TABLE `next_actions_new` RENAME TO `next_actions`")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_next_actions_aiResultId` ON `next_actions` (`aiResultId`)")
        }

        val MIGRATION_3_5 = object : Migration(3, 5) {
            override fun migrate(db: SupportSQLiteDatabase) = addSummaryColumns(db)
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                addSummaryColumns(db)
                dropNextActionsCreatedAt(db)
            }
        }

        fun getDatabase(context: Context): SuiteNDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SuiteNDatabase::class.java,
                    "suiten.db"
                )
                    .addMigrations(MIGRATION_3_5, MIGRATION_4_5)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
