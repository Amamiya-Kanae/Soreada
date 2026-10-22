package com.kanae.soreada.reader

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import com.kanae.soreada.format.TextSection
import kotlin.math.min

/** Shared typography ratios so pagination and Compose rendering agree. */
object TextTypo {
    const val BODY_LINE_HEIGHT_MULT = 1.45f
    const val HEADING_SCALE = 1.30f
    const val HEADING_LINE_HEIGHT_MULT = 1.35f
    const val HEADING_GAP_EM = 0.80f
    const val HORIZONTAL_PADDING_DP = 30f
    const val VERTICAL_PADDING_DP = 22f
    const val HEADING_MAX_LINES = 2
}

data class TextPage(
    val sectionIndex: Int,
    val start: Int,
    val end: Int,
    val text: String,
    /** Character offset from the beginning of the whole book. */
    val absStart: Int,
    val absEnd: Int,
    val chapterTitle: String?,
    val showTitle: Boolean,
)

/**
 * Lays out text with the platform text engine, then splits it into pages on
 * exact line boundaries. The reader renders each page with Compose text using
 * the same font metrics, so a page never spills over.
 */
object TextPaginator {

    fun paginate(
        sections: List<TextSection>,
        widthPx: Int,
        heightPx: Int,
        bodyPx: Float,
    ): List<TextPage> {
        if (widthPx <= 0 || heightPx <= 0 || bodyPx <= 0f) return emptyList()

        val bodyPaint = TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply {
            textSize = bodyPx
            typeface = Typeface.SERIF
        }
        val bodyLineHeight = bodyPx * TextTypo.BODY_LINE_HEIGHT_MULT

        val headingPx = bodyPx * TextTypo.HEADING_SCALE
        val headingPaint = TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply {
            textSize = headingPx
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        val headingLineHeight = headingPx * TextTypo.HEADING_LINE_HEIGHT_MULT
        val headingGap = bodyPx * TextTypo.HEADING_GAP_EM

        // Compose draws every line at exactly bodyLineHeight, while the platform
        // line breaker may report taller lines because of CJK fallback fonts.
        // Count lines instead of summing measured heights, and keep two lines
        // of safety against small wrapping differences.
        val bottomSafety = bodyLineHeight * 2f
        val regularMaxLines = ((heightPx - bottomSafety) / bodyLineHeight)
            .toInt()
            .coerceAtLeast(1)

        val pages = mutableListOf<TextPage>()
        var absCursor = 0

        for ((sectionIndex, section) in sections.withIndex()) {
            val text = section.text
            if (text.isEmpty()) continue
            val title = section.title
            var pageStart = 0
            var firstPageOfSection = true
            var linesOnPage = 0
            var maxLines = if (!title.isNullOrBlank()) {
                ((heightPx - bottomSafety - headingReserve(
                    title,
                    headingPaint,
                    widthPx,
                    headingLineHeight,
                    headingGap,
                )) / bodyLineHeight).toInt().coerceAtLeast(1)
            } else {
                regularMaxLines
            }

            fun emit(end: Int) {
                if (end <= pageStart) return
                pages += TextPage(
                    sectionIndex = sectionIndex,
                    start = pageStart,
                    end = end,
                    text = text.substring(pageStart, end),
                    absStart = absCursor + pageStart,
                    absEnd = absCursor + end,
                    chapterTitle = title?.takeIf { firstPageOfSection },
                    showTitle = firstPageOfSection && !title.isNullOrBlank(),
                )
                firstPageOfSection = false
            }

            var cursor = 0
            while (cursor < text.length) {
                val chunkEnd = nextChunkEnd(text, cursor)
                val chunk = text.substring(cursor, chunkEnd)
                val layout = buildLayout(
                    text = chunk,
                    paint = bodyPaint,
                    width = widthPx,
                )
                for (line in 0 until layout.lineCount) {
                    val lineStart = cursor + layout.getLineStart(line)
                    val lineEnd = cursor + layout.getLineEnd(line)
                    if (lineEnd <= pageStart) continue
                    if (linesOnPage >= maxLines && lineStart > pageStart) {
                        emit(lineStart)
                        pageStart = lineStart
                        maxLines = regularMaxLines
                        linesOnPage = 0
                    }
                    linesOnPage += 1
                }
                cursor = chunkEnd
            }
            emit(text.length)
            absCursor += text.length
        }
        return pages
    }

    @SuppressLint("WrongConstant")
    private fun headingReserve(
        title: String,
        paint: TextPaint,
        width: Int,
        lineHeight: Float,
        gap: Float,
    ): Float {
        val layout = StaticLayout.Builder
            .obtain(title, 0, title.length, paint, width)
            .setIncludePad(false)
            .setMaxLines(TextTypo.HEADING_MAX_LINES)
            .setEllipsize(TextUtils.TruncateAt.END)
            .setBreakStrategy(Layout.BREAK_STRATEGY_SIMPLE)
            .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE)
            .build()
        val lines = min(TextTypo.HEADING_MAX_LINES, layout.lineCount).coerceAtLeast(1)
        return lines * lineHeight + gap
    }

    @SuppressLint("WrongConstant")
    private fun buildLayout(
        text: String,
        paint: TextPaint,
        width: Int,
    ): StaticLayout = StaticLayout.Builder
        .obtain(text, 0, text.length, paint, width)
        .setIncludePad(false)
        .setBreakStrategy(Layout.BREAK_STRATEGY_SIMPLE)
        .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE)
        .build()

    private fun nextChunkEnd(text: String, cursor: Int): Int {
        val minEnd = cursor + 2000
        if (minEnd >= text.length) return text.length
        val maxEnd = min(cursor + 5000, text.length)
        if (maxEnd == text.length) return maxEnd
        val newline = text.indexOf('\n', minEnd)
        return if (newline in minEnd until maxEnd) newline + 1 else maxEnd
    }
}
