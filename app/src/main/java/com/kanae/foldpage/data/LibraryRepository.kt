package com.kanae.foldpage.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.provider.OpenableColumns
import com.kanae.foldpage.format.BookContent
import com.kanae.foldpage.format.BookParseException
import com.kanae.foldpage.format.EpubParser
import com.kanae.foldpage.format.ParsedBook
import com.kanae.foldpage.format.TextParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.util.UUID
import kotlin.math.max

class LibraryRepository(private val context: Context) {

    private val rootDir = File(context.filesDir, "foldpage")
    private val booksDir = File(rootDir, "books")
    private val coversDir = File(rootDir, "covers")
    private val indexFile = File(rootDir, "library.json")
    private val mutex = Mutex()

    @Volatile
    private var cache: List<Book>? = null

    init {
        booksDir.mkdirs()
        coversDir.mkdirs()
    }

    fun bookFile(book: Book): File = File(booksDir, book.fileName)

    fun coverFile(book: Book): File? = book.coverFileName?.let { File(coversDir, it) }?.takeIf { it.exists() }

    suspend fun listBooks(): List<Book> = withContext(Dispatchers.IO) {
        mutex.withLock { readLocked() }
            .sortedByDescending { maxOf(it.lastOpenedAt, it.addedAt) }
    }

    suspend fun getBook(id: String): Book? = withContext(Dispatchers.IO) {
        mutex.withLock { readLocked() }.firstOrNull { it.id == id }
    }

    suspend fun importFromUri(uri: Uri): Book = withContext(Dispatchers.IO) {
        val displayName = queryDisplayName(uri) ?: "未命名"
        val mime = runCatching { context.contentResolver.getType(uri) }.getOrNull()
        val temp = File.createTempFile("import-", ".tmp", booksDir)
        try {
            val input = context.contentResolver.openInputStream(uri)
                ?: throw BookParseException("無法讀取這個檔案，請再試一次。")
            input.use { source ->
                temp.outputStream().use { target -> source.copyTo(target, 128 * 1024) }
            }
            if (temp.length() == 0L) throw BookParseException("這個檔案是空的。")
            if (temp.length() > MAX_IMPORT_BYTES) {
                throw BookParseException("檔案太大（超過 300 MB），暫時無法匯入。")
            }
            val format = detectFormat(displayName, mime, temp)
            importTemp(temp, displayName, format)
        } catch (e: Exception) {
            temp.delete()
            if (e is BookParseException) throw e
            throw BookParseException("匯入失敗：${e.message ?: "未知錯誤"}", e)
        }
    }

    /** Used by the debug-only test import screen. */
    suspend fun importFromFile(source: File, displayName: String): Book = withContext(Dispatchers.IO) {
        val temp = File.createTempFile("import-", ".tmp", booksDir)
        try {
            source.copyTo(temp, overwrite = true)
            val format = detectFormat(displayName, null, temp)
            importTemp(temp, displayName, format)
        } catch (e: Exception) {
            temp.delete()
            if (e is BookParseException) throw e
            throw BookParseException("匯入失敗：${e.message ?: "未知錯誤"}", e)
        }
    }

    private suspend fun importTemp(temp: File, displayName: String, format: BookFormat): Book {
        val id = UUID.randomUUID().toString()
        val fallbackTitle = displayName.substringBeforeLast('.').trim().ifBlank { "未命名" }
        val parsed: ParsedBook = when (format) {
            BookFormat.EPUB -> EpubParser.parse(temp)
            BookFormat.TXT -> TextParser.parse(temp, fallbackTitle)
        }
        val kind = when (parsed.content) {
            is BookContent.Images -> ContentKind.IMAGE
            is BookContent.Text -> ContentKind.TEXT
        }
        val ext = if (format == BookFormat.EPUB) "epub" else "txt"
        val destination = File(booksDir, "$id.$ext")
        if (!temp.renameTo(destination)) {
            temp.copyTo(destination, overwrite = true)
            temp.delete()
        }
        val coverName = parsed.coverBytes?.let { saveCover(id, it) }
        val total = when (val content = parsed.content) {
            is BookContent.Images -> content.pages.size
            is BookContent.Text -> content.totalChars
        }
        val title = parsed.title?.trim()?.takeIf { it.isNotEmpty() } ?: fallbackTitle
        val now = System.currentTimeMillis()
        val book = Book(
            id = id,
            title = title,
            author = parsed.author?.trim()?.takeIf { it.isNotEmpty() },
            fileName = destination.name,
            format = format,
            contentKind = kind,
            coverFileName = coverName,
            addedAt = now,
            lastOpenedAt = now,
            position = 0,
            total = total,
            pageDirection = parsed.direction,
        )
        mutex.withLock {
            val books = readLocked().toMutableList()
            books.add(book)
            writeLocked(books)
        }
        return book
    }

    suspend fun delete(book: Book) = withContext(Dispatchers.IO) {
        bookFile(book).delete()
        coverFile(book)?.delete()
        mutex.withLock {
            val books = readLocked().filterNot { it.id == book.id }
            writeLocked(books)
        }
    }

    suspend fun markOpened(id: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val books = readLocked().toMutableList()
            val index = books.indexOfFirst { it.id == id }
            if (index >= 0) {
                books[index] = books[index].copy(lastOpenedAt = System.currentTimeMillis())
                writeLocked(books)
            }
        }
    }

    suspend fun updateProgress(id: String, position: Int, total: Int) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val books = readLocked().toMutableList()
            val index = books.indexOfFirst { it.id == id }
            if (index >= 0) {
                val safeTotal = if (total > 0) total else books[index].total
                val safePosition = position.coerceIn(0, safeTotal.coerceAtLeast(0))
                books[index] = books[index].copy(position = safePosition, total = safeTotal)
                writeLocked(books)
            }
        }
    }

    suspend fun restart(book: Book): Book = withContext(Dispatchers.IO) {
        mutex.withLock {
            val books = readLocked().toMutableList()
            val index = books.indexOfFirst { it.id == book.id }
            if (index < 0) return@withLock book
            val updated = books[index].copy(position = 0, lastOpenedAt = System.currentTimeMillis())
            books[index] = updated
            writeLocked(books)
            updated
        }
    }

    private fun detectFormat(displayName: String, mime: String?, file: File): BookFormat {
        val lower = displayName.lowercase()
        if (lower.endsWith(".epub")) return BookFormat.EPUB
        if (lower.endsWith(".txt")) return BookFormat.TXT
        if (mime == "application/epub+zip") return BookFormat.EPUB
        if (mime == "text/plain") return BookFormat.TXT
        val header = ByteArray(4)
        val read = file.inputStream().use { it.read(header) }
        if (read >= 2 && header[0] == 'P'.code.toByte() && header[1] == 'K'.code.toByte()) {
            return BookFormat.EPUB
        }
        throw BookParseException("只支援 EPUB 或 TXT 檔案。")
    }

    private fun queryDisplayName(uri: Uri): String? = runCatching {
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull()

    private fun saveCover(id: String, bytes: ByteArray): String? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (bounds.outWidth / (sample * 2) >= 700 && bounds.outHeight / (sample * 2) >= 700) {
            sample *= 2
        }
        val decoded = runCatching {
            BitmapFactory.decodeByteArray(
                bytes,
                0,
                bytes.size,
                BitmapFactory.Options().apply {
                    inSampleSize = sample
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                },
            )
        }.getOrNull() ?: return null

        val targetWidth = 640
        val targetHeight = 960
        val scale = minOf(
            targetWidth.toFloat() / decoded.width,
            targetHeight.toFloat() / decoded.height,
            1f,
        )
        val scaled = if (scale < 0.99f) {
            Bitmap.createScaledBitmap(
                decoded,
                (decoded.width * scale).toInt().coerceAtLeast(1),
                (decoded.height * scale).toInt().coerceAtLeast(1),
                true,
            )
        } else {
            decoded
        }

        val flattened = Bitmap.createBitmap(scaled.width, scaled.height, Bitmap.Config.ARGB_8888)
        Canvas(flattened).apply {
            drawColor(Color.WHITE)
            drawBitmap(scaled, 0f, 0f, null)
        }
        if (scaled !== decoded) decoded.recycle()
        return try {
            val name = "$id.jpg"
            val output = File(coversDir, name)
            output.outputStream().use { stream ->
                flattened.compress(Bitmap.CompressFormat.JPEG, 88, stream)
            }
            name
        } finally {
            flattened.recycle()
            if (scaled !== flattened) scaled.recycle()
        }
    }

    private fun readLocked(): List<Book> {
        cache?.let { return it }
        val books = runCatching {
            if (!indexFile.exists()) emptyList() else {
                val array = JSONArray(indexFile.readText())
                (0 until array.length()).mapNotNull { index ->
                    runCatching { Book.fromJson(array.getJSONObject(index)) }.getOrNull()
                }
            }
        }.getOrDefault(emptyList())
        cache = books
        return books
    }

    private fun writeLocked(books: List<Book>) {
        runCatching {
            val array = JSONArray()
            books.forEach { array.put(it.toJson()) }
            val temp = File(rootDir, "library.json.tmp")
            temp.writeText(array.toString())
            if (!temp.renameTo(indexFile)) {
                indexFile.writeText(array.toString())
                temp.delete()
            }
            cache = books
        }
    }

    private companion object {
        const val MAX_IMPORT_BYTES = 300L * 1024 * 1024
    }
}
