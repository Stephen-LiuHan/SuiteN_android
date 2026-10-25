package space.suiten.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import space.suiten.domain.model.Book
import space.suiten.domain.model.BookSummaryActions
import space.suiten.domain.model.ContentType
import space.suiten.domain.repository.BookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID

class BookViewModel(
    private val bookRepository: BookRepository
) : ViewModel() {

    private val _books = MutableStateFlow<List<Book>>(emptyList())
    val books: StateFlow<List<Book>> = _books.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadBooks()
    }

    fun loadBooks() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                bookRepository.getAllBooks().collect { bookList ->
                    _books.value = bookList
                }
            } catch (e: Exception) {
                // Database errors (e.g. a failed migration) would otherwise only show as an empty shelf
                android.util.Log.e("BookViewModel", "Failed to load books", e)
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadRecentBooks(limit: Int = 5) {
        viewModelScope.launch {
            try {
                bookRepository.getRecentBooks(limit).collect { bookList ->
                    _books.value = bookList
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun createBook(
        contentType: ContentType,
        title: String,
        author: String?,
        customTypeName: String? = null,
        url: String? = null,
        memo: String? = null
    ) {
        viewModelScope.launch {
            try {
                val now = Date()
                val book = Book(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    author = author,
                    summary = null,
                    contentType = contentType,
                    customTypeName = customTypeName,
                    url = url,
                    memo = memo,
                    createdAt = now,
                    updatedAt = now
                )
                bookRepository.createBook(book)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun updateBookContent(
        book: Book,
        title: String,
        author: String?,
        customTypeName: String?,
        url: String?,
        memo: String?
    ) {
        viewModelScope.launch {
            try {
                val updatedBook = book.copy(
                    title = title,
                    author = author,
                    customTypeName = customTypeName,
                    url = url,
                    memo = memo,
                    updatedAt = Date()
                )
                bookRepository.updateBook(updatedBook)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun updateBook(book: Book) {
        viewModelScope.launch {
            try {
                val updatedBook = book.copy(updatedAt = Date())
                bookRepository.updateBook(updatedBook)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun recordSummaryGenerated(book: Book, summary: String, captureCount: Int) =
        updateSummary(book) { BookSummaryActions.generated(it, summary, captureCount) }

    fun saveSummary(book: Book) = updateSummary(book) { BookSummaryActions.saved(it) }

    fun editSummary(book: Book, summary: String) =
        updateSummary(book) { BookSummaryActions.edited(it, summary) }

    fun rateSummary(book: Book, helpfulness: Int) =
        updateSummary(book) { BookSummaryActions.rated(it, helpfulness) }

    // Reads the stored book first so a quick save-then-rate doesn't overwrite the earlier change.
    private fun updateSummary(book: Book, change: (Book) -> Book) {
        viewModelScope.launch {
            try {
                val current = bookRepository.getBookById(book.id) ?: book
                bookRepository.updateBook(change(current))
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun deleteBook(bookId: String) {
        viewModelScope.launch {
            try {
                bookRepository.deleteBook(bookId)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
