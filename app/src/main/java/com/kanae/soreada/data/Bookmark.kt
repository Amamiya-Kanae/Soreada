package com.kanae.soreada.data

import org.json.JSONObject

/**
 * A saved position inside a book.
 *
 * [position] is a page index for image books, or a character offset for text books.
 */
data class Bookmark(
    val position: Int,
    val createdAt: Long,
    val label: String? = null,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("position", position)
        put("createdAt", createdAt)
        put("label", label)
    }

    companion object {
        fun fromJson(json: JSONObject): Bookmark? = runCatching {
            Bookmark(
                position = json.getInt("position"),
                createdAt = json.optLong("createdAt"),
                label = json.optString("label").takeIf { it.isNotBlank() && it != "null" },
            )
        }.getOrNull()
    }
}
