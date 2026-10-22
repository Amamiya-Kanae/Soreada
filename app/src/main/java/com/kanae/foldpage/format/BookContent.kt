package com.kanae.foldpage.format

import com.kanae.foldpage.data.PageDirection

data class ImagePage(
    /** Normalized path of the image inside the EPUB zip file. */
    val entry: String,
    val mediaType: String? = null,
)

data class TextSection(
    val title: String?,
    val text: String,
)

sealed interface BookContent {
    data class Images(val pages: List<ImagePage>) : BookContent

    data class Text(
        val sections: List<TextSection>,
        val totalChars: Int,
    ) : BookContent
}

data class ParsedBook(
    val title: String?,
    val author: String?,
    val direction: PageDirection,
    val coverBytes: ByteArray?,
    val content: BookContent,
)

enum class ParseError {
    FILE_READ,
    NOT_EPUB,
    EMPTY_FILE,
    TOO_LARGE,
    UNSUPPORTED_FORMAT,
    EPUB_CONTAINER,
    EPUB_OPF,
    EPUB_NO_SPINE,
    EPUB_NO_CONTENT,
    TXT_EMPTY,
    BOOK_MISSING,
    FILE_MISSING,
    UNKNOWN,
}

class BookParseException(
    val kind: ParseError,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
