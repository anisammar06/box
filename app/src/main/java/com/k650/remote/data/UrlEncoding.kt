package com.k650.remote.data

/**
 * RFC 3986 percent-encoding.
 *
 * THE MAIN GOTCHA of the WAM/UIC API: query values must use `%20` for spaces.
 * [java.net.URLEncoder] performs *form* encoding (spaces become `+`), which the
 * firmware rejects with errcode 53 "Input parameter/parameters not found".
 *
 * This encoder leaves only the RFC 3986 unreserved set unescaped
 * (`A-Z a-z 0-9 - _ . ~`) and percent-encodes everything else byte-by-byte
 * over UTF-8 — so `<`, `>`, `"`, `/`, `=` and, crucially, space all become the
 * `%NN` form the bar expects.
 */
object UrlEncoding {

    private const val UNRESERVED =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_.~"

    private val HEX = "0123456789ABCDEF".toCharArray()

    fun encode(value: String): String {
        val out = StringBuilder(value.length * 3)
        for (byte in value.toByteArray(Charsets.UTF_8)) {
            val c = byte.toInt() and 0xFF
            if (c.toChar() in UNRESERVED) {
                out.append(c.toChar())
            } else {
                out.append('%')
                out.append(HEX[c ushr 4])
                out.append(HEX[c and 0x0F])
            }
        }
        return out.toString()
    }
}
