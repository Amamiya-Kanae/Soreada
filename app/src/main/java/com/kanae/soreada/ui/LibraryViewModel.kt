package com.kanae.soreada.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kanae.soreada.data.Book
import com.kanae.soreada.data.LibraryRepository
import com.kanae.soreada.format.BookParseException
import com.kanae.soreada.format.ParseError
import com.kanae.soreada.ui.i18n.UiKey
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LibraryUiState(
    val loading: Boolean = true,
    val books: List<Book> = emptyList(),
    val importing: Boolean = false,
)

sealed interface LibraryEvent {
    data class OpenBook(val bookId: String) : LibraryEvent
    data class Message(val key: UiKey, val args: List<Any> = emptyList()) : LibraryEvent
    data class Failure(val error: ParseError, val detail: String? = null) : LibraryEvent
}

class LibraryViewModel(private val repository: LibraryRepository) : ViewModel() {

    private val _state = MutableStateFlow(LibraryUiState())
    val state = _state.asStateFlow()

    private val _events = Channel<LibraryEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        refresh(showLoading = true)
    }

    fun refresh(showLoading: Boolean = false) {
        viewModelScope.launch {
            if (showLoading) _state.update { it.copy(loading = true) }
            runCatching { repository.listBooks() }
                .onSuccess { books ->
                    _state.update { it.copy(books = books, loading = false) }
                }
                .onFailure {
                    _state.update { it.copy(loading = false) }
                    _events.send(LibraryEvent.Failure(ParseError.UNKNOWN, it.message))
                }
        }
    }

    fun import(uri: Uri, openAfter: Boolean = false) {
        if (_state.value.importing) return
        viewModelScope.launch {
            _state.update { it.copy(importing = true) }
            try {
                val book = repository.importFromUri(uri)
                val books = repository.listBooks()
                _state.update { it.copy(books = books, importing = false) }
                if (openAfter) {
                    _events.send(LibraryEvent.OpenBook(book.id))
                } else {
                    _events.send(
                        LibraryEvent.Message(UiKey.IMPORTED_BOOK, listOf(book.title)),
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(importing = false) }
                val parse = e as? BookParseException
                _events.send(
                    LibraryEvent.Failure(
                        error = parse?.kind ?: ParseError.UNKNOWN,
                        detail = e.message,
                    ),
                )
            }
        }
    }

    fun delete(book: Book) {
        viewModelScope.launch {
            runCatching { repository.delete(book) }
                .onSuccess {
                    _state.update { state -> state.copy(books = state.books.filterNot { it.id == book.id }) }
                    _events.send(LibraryEvent.Message(UiKey.REMOVED_BOOK, listOf(book.title)))
                }
                .onFailure { _events.send(LibraryEvent.Message(UiKey.DELETE_FAILED)) }
        }
    }

    fun restart(book: Book, onRestarted: (Book) -> Unit) {
        viewModelScope.launch {
            runCatching { repository.restart(book) }
                .onSuccess { updated ->
                    _state.update { state ->
                        state.copy(books = state.books.map { if (it.id == updated.id) updated else it })
                    }
                    onRestarted(updated)
                }
                .onFailure { _events.send(LibraryEvent.Message(UiKey.RESET_FAILED)) }
        }
    }

    fun markOpened(book: Book) {
        viewModelScope.launch {
            runCatching { repository.markOpened(book.id) }
            _state.update { state ->
                state.copy(
                    books = state.books.map {
                        if (it.id == book.id) it.copy(lastOpenedAt = System.currentTimeMillis()) else it
                    },
                )
            }
        }
    }
}

class LibraryViewModelFactory(private val repository: LibraryRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return LibraryViewModel(repository) as T
    }
}
