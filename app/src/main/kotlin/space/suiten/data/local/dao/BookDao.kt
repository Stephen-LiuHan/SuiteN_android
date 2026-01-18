package space.suiten.data.local.dao

import androidx.room.*
import space.suiten.data.local.entity.BookEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY updatedAt DESC")
    fun getAllBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getBookById(id: String): BookEntity?

    @Query("SELECT * FROM books ORDER BY updatedAt DESC LIMIT :limit")
    fun getRecentBooks(limit: Int): Flow<List<BookEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity)

    @Update
    suspend fun updateBook(book: BookEntity)

    @Delete
    suspend fun deleteBook(book: BookEntity)

    @Query("DELETE FROM books WHERE id = :id")
    suspend fun deleteBookById(id: String)

    @Query("SELECT * FROM books WHERE updatedAt > :sinceTimestamp")
    suspend fun getBooksUpdatedSince(sinceTimestamp: Long): List<BookEntity>

    @Query("SELECT * FROM books")
    suspend fun getAllBooksSync(): List<BookEntity>
}
