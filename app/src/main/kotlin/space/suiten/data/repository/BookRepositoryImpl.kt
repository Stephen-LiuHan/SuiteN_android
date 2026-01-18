package space.suiten.data.repository

import space.suiten.data.local.dao.BookDao
import space.suiten.data.local.entity.BookEntity
import space.suiten.domain.model.Book
import space.suiten.domain.repository.BookRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BookRepositoryImpl(
    private val bookDao: BookDao
) : BookRepository {

    override fun getAllBooks(): Flow<List<Book>> {
        return bookDao.getAllBooks().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getRecentBooks(limit: Int): Flow<List<Book>> {
        return bookDao.getRecentBooks(limit).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getBookById(id: String): Book? {
        return bookDao.getBookById(id)?.toDomain()
    }

    override suspend fun createBook(book: Book) {
        bookDao.insertBook(BookEntity.fromDomain(book))
    }

    override suspend fun updateBook(book: Book) {
        bookDao.updateBook(BookEntity.fromDomain(book))
    }

    override suspend fun deleteBook(id: String) {
        bookDao.deleteBookById(id)
    }
}
