package com.kanae.foldpage.format

import com.kanae.foldpage.data.PageDirection
import java.io.File

object TextParser {

    private val chapterHeading = Regex(
        """^\s*(第[0-9零〇一二三四五六七八九十百千萬万两兩]+[章回節节卷集篇部][^\n]{0,40}|序章[^\n]{0,40}|楔子[^\n]{0,40}|引子[^\n]{0,40}|前言[^\n]{0,40}|後記[^\n]{0,40}|后记[^\n]{0,40}|番外[^\n]{0,40}|Chapter\s+\d+[^\n]{0,48}|CHAPTER\s+\d+[^\n]{0,48})\s*$""",
        RegexOption.IGNORE_CASE,
    )

    fun parse(file: File, fallbackTitle: String): ParsedBook {
        val text = normalize(TextDecoding.decode(file.readBytes()))
        val sections = splitSections(text)
        if (sections.isEmpty()) {
            throw BookParseException(ParseError.TXT_EMPTY, "這個文字檔看起來是空的。")
        }
        return ParsedBook(
            title = fallbackTitle,
            author = null,
            direction = PageDirection.LTR,
            coverBytes = null,
            content = BookContent.Text(
                sections = sections,
                totalChars = sections.sumOf { it.text.length },
            ),
        )
    }

    fun splitSections(rawText: String): List<TextSection> {
        val text = normalize(rawText)
        if (text.isBlank()) return emptyList()

        val sections = mutableListOf<TextSection>()
        val body = StringBuilder()
        var currentTitle: String? = null

        fun flush() {
            val value = body.toString().trim('\n')
            if (value.isNotBlank()) {
                sections += TextSection(currentTitle, normalizeBody(value))
            } else if (currentTitle != null && sections.isNotEmpty()) {
                // A heading with no body before the next heading: keep it attached to the next section.
            }
            body.setLength(0)
        }

        text.split('\n').forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isNotEmpty() && chapterHeading.matches(trimmed)) {
                flush()
                currentTitle = trimmed
            } else {
                body.append(line).append('\n')
            }
        }
        flush()

        if (sections.isEmpty()) {
            return listOf(TextSection(null, text.trim()))
        }
        return sections
    }

    private fun normalize(text: String): String =
        text.replace("\r\n", "\n").replace('\r', '\n').removePrefix("\uFEFF")

    private fun normalizeBody(text: String): String =
        text.replace(Regex("\n{3,}"), "\n\n").trim()
}
