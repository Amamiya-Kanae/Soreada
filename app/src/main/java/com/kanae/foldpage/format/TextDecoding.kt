package com.kanae.foldpage.format

import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

object TextDecoding {

    private val utf8 = Charsets.UTF_8
    private val gb18030 = Charset.forName("GB18030")
    private val latin1 = Charsets.ISO_8859_1

    fun decode(bytes: ByteArray): String {
        if (bytes.isEmpty()) return ""

        // BOM based detection first.
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
            return String(bytes, 3, bytes.size - 3, utf8)
        }
        if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
            return String(bytes, 2, bytes.size - 2, Charsets.UTF_16LE)
        }
        if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
            return String(bytes, 2, bytes.size - 2, Charsets.UTF_16BE)
        }

        strictDecode(bytes, utf8)?.let { return it }
        strictDecode(bytes, gb18030)?.let { return it }
        return strictDecode(bytes, latin1) ?: String(bytes, latin1)
    }

    /** Decodes an XML/HTML document, respecting a declared encoding when present. */
    fun decodeMarkup(bytes: ByteArray): String {
        if (bytes.isEmpty()) return ""
        val head = String(bytes, 0, minOf(bytes.size, 256), latin1)
        val declared = Regex("""encoding\s*=\s*["']([A-Za-z0-9._\-]+)["']""")
            .find(head)?.groupValues?.getOrNull(1)
        if (declared != null) {
            runCatching {
                val charset = Charset.forName(declared)
                return String(bytes, charset)
            }
        }
        return decode(bytes)
    }

    private fun strictDecode(bytes: ByteArray, charset: Charset): String? = try {
        charset.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
            .toString()
    } catch (_: CharacterCodingException) {
        null
    } catch (_: Exception) {
        null
    }
}
