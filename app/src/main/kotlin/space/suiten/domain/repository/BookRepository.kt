package space.suiten.domain.repository

import space.suiten.domain.model.Book
import kotlinx.coroutines.flow.Flow

interface BookRepository {
    fun getAllBooks(): Flow<List<Book>>
    fun getRecentBooks(limit: Int): Flow<List<Book>>
    suspend fun getBookById(id: String): Book?
    suspend fun createBook(book: Book)
    suspend fun updateBook(book: Book)
    suspend fun deleteBook(id: String)
}
