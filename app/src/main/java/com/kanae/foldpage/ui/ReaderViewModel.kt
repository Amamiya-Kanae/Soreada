package com.kanae.foldpage.ui

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kanae.foldpage.data.Book
import com.kanae.foldpage.data.ContentKind
import com.kanae.foldpage.data.DirectionSetting
import com.kanae.foldpage.data.DualMode
import com.kanae.foldpage.data.LibraryRepository
import com.kanae.foldpage.data.PageDirection
import com.kanae.foldpage.data.ReaderSettings
import com.kanae.foldpage.data.ReaderSettingsStore
import com.kanae.foldpage.format.BookContent
import com.kanae.foldpage.format.EpubParser
import com.kanae.foldpage.format.ImagePage
import com.kanae.foldpage.format.TextParser
import com.kanae.foldpage.format.TextSection
import com.kanae.foldpage.reader.PageImageLoader
import com.kanae.foldpage.reader.TextPage
import com.kanae.foldpage.reader.TextPaginator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ReaderUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val book: Book? = null,
    val contentKind: ContentKind = ContentKind.IMAGE,
    val imagePages: List<ImagePage> = emptyList(),
    val textSections: List<TextSection> = emptyList(),
    val textPages: List<TextPage> = emptyList(),
    val paginating: Boolean = false,
    val page: Int = 0,
    val pageCount: Int = 0,
    val direction: PageDirection = PageDirection.LTR,
    val autoDirection: PageDirection = PageDirection.LTR,
    val totalChars: Int = 0,
    val settings: ReaderSettings = ReaderSettings(),
) {
    val isText: Boolean get() = contentKind == ContentKind.TEXT
}

class ReaderViewModel(
    private val repository: LibraryRepository,
    private val bookId: String,
    private val settingsStore: ReaderSettingsStore,
) : ViewModel() {

    private val _state = MutableStateFlow(ReaderUiState())
    val state = _state.asStateFlow()

    private var imageLoader: PageImageLoader? = null
    private var loadJob: Job? = null
    private var paginateJob: Job? = null
    private var saveJob: Job? = null
    private val persistScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var offset: Int = 0
    private var paginationKey: PaginationKey? = null
    private var parsedDirection: PageDirection = PageDirection.LTR

    private data class PaginationKey(val width: Int, val height: Int, val bodyPx: Float)

    init {
        load()
    }

    fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val book = repository.getBook(bookId) ?: error("找不到這本書，可能已被移除。")
                val file = repository.bookFile(book)
                if (!file.exists()) error("書籍檔案不存在，請重新匯入。")

                val parsed = when (book.format) {
                    com.kanae.foldpage.data.BookFormat.EPUB -> EpubParser.parse(file)
                    com.kanae.foldpage.data.BookFormat.TXT -> TextParser.parse(file, book.title)
                }
                parsedDirection = parsed.direction
                val settings = settingsStore.load()
                val content = parsed.content
                val kind = when (content) {
                    is BookContent.Images -> ContentKind.IMAGE
                    is BookContent.Text -> ContentKind.TEXT
                }
                val direction = effectiveDirection(settings, parsed.direction)

                when (content) {
                    is BookContent.Images -> {
                        imageLoader?.close()
                        imageLoader = PageImageLoader(file)
                        val pages = content.pages
                        val position = book.position.coerceIn(0, (pages.size - 1).coerceAtLeast(0))
                        offset = position
                        _state.value = ReaderUiState(
                            loading = false,
                            book = book,
                            contentKind = kind,
                            imagePages = pages,
                            page = position,
                            pageCount = pages.size,
                            direction = direction,
                            autoDirection = parsed.direction,
                            settings = settings,
                        )
                    }
                    is BookContent.Text -> {
                        offset = book.position.coerceIn(0, content.totalChars)
                        _state.value = ReaderUiState(
                            loading = false,
                            book = book,
                            contentKind = kind,
                            textSections = content.sections,
                            page = 0,
                            pageCount = 0,
                            direction = direction,
                            autoDirection = parsed.direction,
                            totalChars = content.totalChars,
                            settings = settings,
                        )
                    }
                }
                repository.markOpened(bookId)
            } catch (e: Exception) {
                _state.value = ReaderUiState(
                    loading = false,
                    error = e.message ?: "無法開啟這本書。",
                    book = _state.value.book,
                )
            }
        }
    }

    fun paginate(widthPx: Int, heightPx: Int, bodyPx: Float) {
        val sections = _state.value.textSections
        if (sections.isEmpty() || widthPx <= 0 || heightPx <= 0) return
        val key = PaginationKey(widthPx, heightPx, bodyPx)
        if (key == paginationKey) return
        paginationKey = key
        paginateJob?.cancel()
        _state.update { it.copy(paginating = true) }
        paginateJob = viewModelScope.launch {
            val pages = withContext(Dispatchers.Default) {
                TextPaginator.paginate(sections, widthPx, heightPx, bodyPx)
            }
            if (paginationKey != key) return@launch
            if (pages.isEmpty()) {
                _state.update { it.copy(paginating = false, pageCount = 0) }
                return@launch
            }
            val target = pages.indexOfLast { it.absStart <= offset }
                .let { if (it < 0) 0 else it }
                .coerceAtMost(pages.lastIndex)
            offset = pages[target].absStart
            _state.update {
                it.copy(
                    textPages = pages,
                    paginating = false,
                    page = target,
                    pageCount = pages.size,
                )
            }
            scheduleSave()
        }
    }

    fun setDirection(setting: DirectionSetting) {
        val settings = _state.value.settings.copy(direction = setting)
        settingsStore.save(settings)
        _state.update {
            it.copy(settings = settings, direction = effectiveDirection(settings, parsedDirection))
        }
    }

    fun setDualMode(mode: DualMode) {
        val settings = _state.value.settings.copy(dualMode = mode)
        settingsStore.save(settings)
        _state.update { it.copy(settings = settings) }
    }

    fun setFontSize(sizeSp: Int) {
        val safe = sizeSp.coerceIn(14, 30)
        if (safe == _state.value.settings.fontSizeSp) return
        val settings = _state.value.settings.copy(fontSizeSp = safe)
        settingsStore.save(settings)
        paginationKey = null
        _state.update { it.copy(settings = settings) }
    }

    /** Called with the first page index of the settled spread. */
    fun onPagerSettled(page: Int, pagesPerSpread: Int) {
        val count = _state.value.pageCount
        if (count <= 0) return
        val safe = page.coerceIn(0, count - 1)
        val current = _state.value.page
        val textPage = _state.value.textPages.getOrNull(safe)
        if (_state.value.isText && textPage != null) offset = textPage.absStart
        if (safe != current) {
            _state.update { it.copy(page = safe) }
        }
        if (safe != current || !_state.value.isText) scheduleSave()
    }

    fun jumpTo(page: Int) {
        val count = _state.value.pageCount
        if (count <= 0) return
        val safe = page.coerceIn(0, count - 1)
        _state.value.textPages.getOrNull(safe)?.let { offset = it.absStart }
        if (safe != _state.value.page) {
            _state.update { it.copy(page = safe) }
        }
        scheduleSave()
    }

    suspend fun loadBitmap(entry: String, width: Int, height: Int): Bitmap? =
        imageLoader?.load(entry, width, height)

    fun prefetch(entries: List<String>, width: Int, height: Int) {
        val loader = imageLoader ?: return
        viewModelScope.launch(Dispatchers.IO) {
            entries.forEach { entry -> loader.load(entry, width, height) }
        }
    }

    fun flushProgress() {
        saveJob?.cancel()
        saveNow()
    }

    fun close() {
        saveJob?.cancel()
        saveNow()
        paginateJob?.cancel()
        imageLoader?.close()
        imageLoader = null
    }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(400)
            saveNow()
        }
    }

    private fun saveNow() {
        val current = _state.value
        val book = current.book ?: return
        val position = if (current.isText) {
            current.textPages.getOrNull(current.page)?.absStart ?: offset
        } else {
            current.page
        }
        val total = if (current.isText) current.totalChars else current.pageCount
        if (total <= 0) return
        persistScope.launch {
            repository.updateProgress(book.id, position, total)
        }
    }

    private fun effectiveDirection(settings: ReaderSettings, auto: PageDirection): PageDirection =
        when (settings.direction) {
            DirectionSetting.AUTO -> auto
            DirectionSetting.LTR -> PageDirection.LTR
            DirectionSetting.RTL -> PageDirection.RTL
        }

    override fun onCleared() {
        super.onCleared()
        close()
    }
}

class ReaderViewModelFactory(
    private val repository: LibraryRepository,
    private val bookId: String,
    private val settingsStore: ReaderSettingsStore,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ReaderViewModel(repository, bookId, settingsStore) as T
    }
}
