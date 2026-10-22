package com.kanae.soreada.ui.i18n

import com.kanae.soreada.format.ParseError

fun ParseError.toUiKey(): UiKey = when (this) {
    ParseError.FILE_READ -> UiKey.ERROR_FILE_READ
    ParseError.NOT_EPUB -> UiKey.ERROR_NOT_EPUB
    ParseError.EMPTY_FILE -> UiKey.ERROR_EMPTY_FILE
    ParseError.TOO_LARGE -> UiKey.ERROR_TOO_LARGE
    ParseError.UNSUPPORTED_FORMAT -> UiKey.ERROR_UNSUPPORTED
    ParseError.EPUB_CONTAINER -> UiKey.ERROR_EPUB_CONTAINER
    ParseError.EPUB_OPF -> UiKey.ERROR_EPUB_OPF
    ParseError.EPUB_NO_SPINE -> UiKey.ERROR_EPUB_NO_SPINE
    ParseError.EPUB_NO_CONTENT -> UiKey.ERROR_EPUB_NO_CONTENT
    ParseError.TXT_EMPTY -> UiKey.ERROR_TXT_EMPTY
    ParseError.BOOK_MISSING -> UiKey.ERROR_BOOK_MISSING
    ParseError.FILE_MISSING -> UiKey.ERROR_FILE_MISSING
    ParseError.UNKNOWN -> UiKey.ERROR_UNKNOWN
}

fun UiStrings.errorText(error: ParseError): String = get(error.toUiKey())
