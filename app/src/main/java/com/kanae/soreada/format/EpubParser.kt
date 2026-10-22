package com.kanae.soreada.format

import android.text.Html
import android.util.Xml
import com.kanae.soreada.data.PageDirection
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.StringReader
import java.net.URLDecoder
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * A small, dependency-free EPUB 2/3 parser aimed at comic pages.
 *
 * It reads the OPF spine, and treats a spine document as an image page when it
 * contains a single image and (almost) no text. Everything else becomes a text
 * section that is later paginated by the reader.
 */
object EpubParser {

    private const val MAX_DOCUMENT_BYTES = 8 * 1024 * 1024
    private const val MAX_COVER_BYTES = 25 * 1024 * 1024

    private data class ManifestItem(
        val id: String,
        val path: String,
        val mediaType: String,
        val properties: String,
    )

    fun parse(file: File): ParsedBook {
        try {
            ZipFile(file).use { zip -> return parseZip(zip) }
        } catch (e: BookParseException) {
            throw e
        } catch (e: Exception) {
            throw BookParseException(ParseError.FILE_READ, "EPUB 檔案讀取失敗，檔案可能損壞或格式不正確。", e)
        }
    }

    private fun parseZip(zip: ZipFile): ParsedBook {
        val containerPath = "META-INF/container.xml"
        val containerBytes = readEntry(zip, containerPath, MAX_DOCUMENT_BYTES)
            ?: throw BookParseException(ParseError.EPUB_CONTAINER, "這不是完整的 EPUB：缺少 META-INF/container.xml。")

        val opfPath = findRootfile(TextDecoding.decodeMarkup(containerBytes))
            ?: throw BookParseException(ParseError.EPUB_OPF, "EPUB 缺少內容目錄（OPF）。")

        val opfBytes = readEntry(zip, opfPath, MAX_DOCUMENT_BYTES)
            ?: throw BookParseException(ParseError.EPUB_OPF, "找不到 EPUB 的內容目錄（OPF）。")
        val opf = parseOpf(TextDecoding.decodeMarkup(opfBytes), opfPath)

        if (opf.spineIds.isEmpty()) {
            throw BookParseException(ParseError.EPUB_NO_SPINE, "這個 EPUB 沒有任何可閱讀的章節。")
        }

        val images = mutableListOf<ImagePage>()
        val sections = mutableListOf<TextSection>()

        for (id in opf.spineIds) {
            val item = opf.manifest[id] ?: continue
            val type = item.mediaType.lowercase()
            if (type.startsWith("image/")) {
                images += ImagePage(item.path, type)
                continue
            }
            if (!isHtml(type, item.path)) continue

            val bytes = readEntry(zip, item.path, MAX_DOCUMENT_BYTES) ?: continue
            val html = TextDecoding.decodeMarkup(bytes)
            val imageRef = findImageReference(html)
            val plain = plainText(html)

            if (imageRef != null && (opf.prePaginated || plain.isBlank())) {
                val path = resolveRelative(item.path, imageRef)
                if (findEntry(zip, path) != null) {
                    images += ImagePage(path, mediaTypeFor(path))
                    continue
                }
            }

            if (plain.isNotBlank()) {
                val sectionTitle = extractTitle(html, opf.title)
                val body = if (sectionTitle != null && plain.startsWith(sectionTitle)) {
                    plain.removePrefix(sectionTitle).trimStart()
                } else {
                    plain
                }
                if (body.isNotBlank()) {
                    sections += TextSection(sectionTitle, body)
                }
            } else if (imageRef != null) {
                val path = resolveRelative(item.path, imageRef)
                if (findEntry(zip, path) != null) {
                    images += ImagePage(path, mediaTypeFor(path))
                }
            }
        }

        val content = when {
            images.isNotEmpty() && sections.isEmpty() -> BookContent.Images(images)
            images.isEmpty() && sections.isNotEmpty() -> textContent(sections)
            images.isNotEmpty() && sections.isNotEmpty() -> {
                if (images.size >= sections.size) BookContent.Images(images) else textContent(sections)
            }
            else -> throw BookParseException(
                ParseError.EPUB_NO_CONTENT,
                "這個 EPUB 中找不到可顯示的內容，可能受 DRM 保護或檔案不完整。",
            )
        }

        val coverBytes = readCover(zip, opf)
        return ParsedBook(
            title = opf.title,
            author = opf.author,
            direction = opf.direction,
            coverBytes = coverBytes,
            content = content,
        )
    }

    private fun textContent(sections: List<TextSection>): BookContent.Text =
        BookContent.Text(sections = sections, totalChars = sections.sumOf { it.text.length })

    private data class Opf(
        val title: String?,
        val author: String?,
        val direction: PageDirection,
        val manifest: Map<String, ManifestItem>,
        val spineIds: List<String>,
        val coverId: String?,
        val coverPath: String?,
        val prePaginated: Boolean,
    )

    private fun findRootfile(containerXml: String): String? {
        var result: String? = null
        forEachXmlTag(containerXml) { event, parser ->
            if (event == XmlPullParser.START_TAG && localName(parser) == "rootfile") {
                result = parser.getAttributeValue(null, "full-path")
            }
        }
        return result?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun parseOpf(opfXml: String, opfPath: String): Opf {
        val manifest = LinkedHashMap<String, ManifestItem>()
        val spineIds = mutableListOf<String>()
        var direction = PageDirection.LTR
        var title: String? = null
        var author: String? = null
        var coverId: String? = null
        var guideCoverPath: String? = null
        var prePaginated = false

        var capturingTitle = false
        var capturingCreator = false
        var capturingLayout = false
        val titleBuilder = StringBuilder()
        val creatorBuilder = StringBuilder()
        val layoutBuilder = StringBuilder()

        forEachXmlTag(opfXml) { event, parser ->
            when (event) {
                XmlPullParser.START_TAG -> when (val tag = localName(parser)) {
                    "item" -> {
                        val id = parser.getAttributeValue(null, "id") ?: return@forEachXmlTag
                        val href = parser.getAttributeValue(null, "href") ?: return@forEachXmlTag
                        manifest[id] = ManifestItem(
                            id = id,
                            path = resolveRelative(opfPath, href),
                            mediaType = parser.getAttributeValue(null, "media-type").orEmpty(),
                            properties = parser.getAttributeValue(null, "properties").orEmpty(),
                        )
                    }
                    "itemref" -> {
                        parser.getAttributeValue(null, "idref")?.let { spineIds += it }
                    }
                    "spine" -> {
                        val value = parser.getAttributeValue(null, "page-progression-direction")
                        if (value?.equals("rtl", ignoreCase = true) == true) {
                            direction = PageDirection.RTL
                        }
                    }
                    "meta" -> {
                        val name = parser.getAttributeValue(null, "name")
                        val content = parser.getAttributeValue(null, "content")
                        if (name == "cover" && !content.isNullOrBlank()) coverId = content
                        val property = parser.getAttributeValue(null, "property")
                        if (property == "rendition:layout") {
                            capturingLayout = true
                            layoutBuilder.setLength(0)
                        }
                    }
                    "reference" -> {
                        val type = parser.getAttributeValue(null, "type")
                        val href = parser.getAttributeValue(null, "href")
                        if (type.equals("cover", ignoreCase = true) && !href.isNullOrBlank()) {
                            guideCoverPath = resolveRelative(opfPath, href)
                        }
                    }
                    "title" -> {
                        capturingTitle = true
                        titleBuilder.setLength(0)
                    }
                    "creator" -> {
                        capturingCreator = true
                        creatorBuilder.setLength(0)
                    }
                }
                XmlPullParser.TEXT -> {
                    if (capturingTitle) titleBuilder.append(parser.text)
                    if (capturingCreator) creatorBuilder.append(parser.text)
                    if (capturingLayout) layoutBuilder.append(parser.text)
                }
                XmlPullParser.END_TAG -> when (localName(parser)) {
                    "title" -> {
                        if (capturingTitle) {
                            title = cleanInline(titleBuilder.toString())
                            capturingTitle = false
                        }
                    }
                    "creator" -> {
                        if (capturingCreator) {
                            author = cleanInline(creatorBuilder.toString())
                            capturingCreator = false
                        }
                    }
                    "meta" -> {
                        if (capturingLayout) {
                            if (layoutBuilder.toString().contains("pre-paginated", ignoreCase = true)) {
                                prePaginated = true
                            }
                            capturingLayout = false
                        }
                    }
                }
            }
        }

        return Opf(
            title = title,
            author = author,
            direction = direction,
            manifest = manifest,
            spineIds = spineIds,
            coverId = coverId,
            coverPath = guideCoverPath,
            prePaginated = prePaginated,
        )
    }

    private fun readCover(zip: ZipFile, opf: Opf): ByteArray? {
        val candidates = buildList {
            opf.manifest.values.firstOrNull { item ->
                item.properties.split(' ').any { it.equals("cover-image", ignoreCase = true) }
            }?.let { add(it.path) }
            opf.coverId?.let { id -> opf.manifest[id]?.let { add(it.path) } }
            opf.coverPath?.let { add(it) }
            opf.manifest.values.firstOrNull { item ->
                item.mediaType.startsWith("image/") && item.path.contains("cover", ignoreCase = true)
            }?.let { add(it.path) }
        }
        for (path in candidates.distinct()) {
            if (!isImagePath(path)) continue
            readEntry(zip, path, MAX_COVER_BYTES)?.let { return it }
        }
        return null
    }

    private fun readEntry(zip: ZipFile, path: String, maxBytes: Int): ByteArray? {
        val entry = findEntry(zip, path) ?: return null
        if (entry.size > maxBytes) return null
        return zip.getInputStream(entry).use { input ->
            val output = java.io.ByteArrayOutputStream(minOf(entry.size.coerceAtLeast(0L), 256 * 1024L).toInt())
            val buffer = ByteArray(64 * 1024)
            var total = 0
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                total += read
                if (total > maxBytes) return null
                output.write(buffer, 0, read)
            }
            output.toByteArray()
        }
    }

    private fun findEntry(zip: ZipFile, rawPath: String): ZipEntry? {
        val path = normalizePath(rawPath)
        zip.getEntry(path)?.let { return it }
        zip.getEntry(rawPath)?.let { return it }
        val decoded = runCatching { percentDecode(path) }.getOrNull()
        if (decoded != null && decoded != path) zip.getEntry(decoded)?.let { return it }
        val encoded = path.replace(" ", "%20")
        if (encoded != path) zip.getEntry(encoded)?.let { return it }
        return null
    }

    private fun isHtml(mediaType: String, path: String): Boolean =
        mediaType.contains("html") || mediaType.contains("xml") ||
            path.endsWith(".xhtml", true) || path.endsWith(".html", true) || path.endsWith(".htm", true)

    private fun isImagePath(path: String): Boolean {
        val lower = path.lowercase()
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") ||
            lower.endsWith(".webp") || lower.endsWith(".gif") || lower.endsWith(".bmp")
    }

    private fun mediaTypeFor(path: String): String = when (path.substringAfterLast('.', "").lowercase()) {
        "png" -> "image/png"
        "webp" -> "image/webp"
        "gif" -> "image/gif"
        "bmp" -> "image/bmp"
        else -> "image/jpeg"
    }

    private fun findImageReference(html: String): String? {
        val img = Regex("""(?is)<img\b[^>]*?\bsrc\s*=\s*(["'])(.*?)\1""")
            .find(html)?.groupValues?.getOrNull(2)
        val svg = Regex("""(?is)<image\b[^>]*?\b(?:xlink:href|href)\s*=\s*(["'])(.*?)\1""")
            .find(html)?.groupValues?.getOrNull(2)
        return (img ?: svg)?.let { decodeEntities(it) }?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun plainText(html: String): String {
        val withoutNoise = html
            .replace(Regex("""(?is)<head\b.*?</head>"""), " ")
            .replace(Regex("""(?is)<style\b.*?</style>"""), " ")
            .replace(Regex("""(?is)<script\b.*?</script>"""), " ")
        val raw = runCatching {
            Html.fromHtml(withoutNoise, Html.FROM_HTML_MODE_LEGACY).toString()
        }.getOrElse { withoutNoise.replace(Regex("<[^>]+>"), " ") }
        return raw
            .replace('\u00A0', ' ')
            .replace("\r\n", "\n")
            .replace(Regex("[ \\t]+\\n"), "\n")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
    }

    private fun extractTitle(html: String, fallback: String?): String? {
        val h1 = Regex("""(?is)<h[12][^>]*>(.*?)</h[12]>""").find(html)?.groupValues?.getOrNull(1)
        val titleTag = Regex("""(?is)<title[^>]*>(.*?)</title>""").find(html)?.groupValues?.getOrNull(1)
        val candidate = listOf(h1, titleTag).filterNotNull().firstNotNullOfOrNull { raw ->
            cleanInline(
                runCatching { Html.fromHtml(raw, Html.FROM_HTML_MODE_LEGACY).toString() }
                    .getOrElse { raw.replace(Regex("<[^>]+>"), " ") },
            ).takeIf { it.isNotEmpty() }
        } ?: return null
        if (candidate.length > 80) return null
        if (fallback != null && candidate.equals(fallback, ignoreCase = true)) return null
        return candidate
    }

    private fun cleanInline(value: String): String =
        value.replace(Regex("\\s+"), " ").trim()

    private fun decodeEntities(value: String): String = value
        .replace("&amp;", "&")
        .replace("&#38;", "&")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
        .replace("&#39;", "'")

    private fun localName(parser: XmlPullParser): String =
        parser.name.orEmpty().substringAfterLast(':').lowercase()

    private inline fun forEachXmlTag(xml: String, block: (Int, XmlPullParser) -> Unit) {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(StringReader(xml))
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            block(event, parser)
            event = parser.next()
        }
    }

    /** Resolves a href relative to [baseEntry] (a path inside the zip). */
    fun resolveRelative(baseEntry: String, href: String): String {
        val raw = href.substringBefore('#').substringBefore('?').trim()
        if (raw.isEmpty()) return normalizePath(baseEntry)
        val decoded = runCatching { percentDecode(raw) }.getOrDefault(raw)
        if (decoded.startsWith("/")) return normalizePath(decoded)
        val baseDir = baseEntry.substringBeforeLast('/', "")
        val combined = if (baseDir.isEmpty()) decoded else "$baseDir/$decoded"
        return normalizePath(combined)
    }

    private fun percentDecode(value: String): String =
        URLDecoder.decode(value.replace("+", "%2B"), "UTF-8")

    private fun normalizePath(path: String): String {
        val parts = path.replace('\\', '/').split('/')
        val stack = ArrayDeque<String>()
        for (part in parts) {
            when (part) {
                "", "." -> Unit
                ".." -> if (stack.isNotEmpty()) stack.removeLast()
                else -> stack.addLast(part)
            }
        }
        return stack.joinToString("/")
    }
}
