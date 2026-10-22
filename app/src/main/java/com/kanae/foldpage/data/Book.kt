package com.kanae.foldpage.data

import org.json.JSONObject

enum class BookFormat { EPUB, TXT }

enum class ContentKind { IMAGE, TEXT }

enum class PageDirection { LTR, RTL }

enum class DirectionSetting { AUTO, LTR, RTL }

enum class DualMode { AUTO, SINGLE, DUAL }

data class ReaderSettings(
    val direction: DirectionSetting = DirectionSetting.AUTO,
    val dualMode: DualMode = DualMode.AUTO,
    val fontSizeSp: Int = 18,
)

/**
 * A book in the library.
 *
 * [position] is a page index for image books, or a character offset for text books.
 * [total] is the page count for image books, or the character count for text books.
 */
data class Book(
    val id: String,
    val title: String,
    val author: String?,
    val fileName: String,
    val format: BookFormat,
    val contentKind: ContentKind,
    val coverFileName: String?,
    val addedAt: Long,
    val lastOpenedAt: Long,
    val position: Int,
    val total: Int,
    val pageDirection: PageDirection,
) {
    val progress: Float
        get() = if (total <= 0) 0f else (position.toFloat() / total.toFloat()).coerceIn(0f, 1f)

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("author", author)
        put("fileName", fileName)
        put("format", format.name)
        put("contentKind", contentKind.name)
        put("coverFileName", coverFileName)
        put("addedAt", addedAt)
        put("lastOpenedAt", lastOpenedAt)
        put("position", position)
        put("total", total)
        put("pageDirection", pageDirection.name)
    }

    companion object {
        fun fromJson(json: JSONObject): Book? = runCatching {
            Book(
                id = json.getString("id"),
                title = json.getString("title"),
                author = json.optString("author").takeIf { it.isNotBlank() && it != "null" },
                fileName = json.getString("fileName"),
                format = BookFormat.valueOf(json.getString("format")),
                contentKind = ContentKind.valueOf(json.getString("contentKind")),
                coverFileName = json.optString("coverFileName").takeIf { it.isNotBlank() && it != "null" },
                addedAt = json.optLong("addedAt"),
                lastOpenedAt = json.optLong("lastOpenedAt"),
                position = json.optInt("position"),
                total = json.optInt("total"),
                pageDirection = runCatching {
                    PageDirection.valueOf(json.optString("pageDirection", "LTR"))
                }.getOrDefault(PageDirection.LTR),
            )
        }.getOrNull()
    }
}
