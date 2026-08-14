package com.k650.remote.data

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader

/**
 * Lenient reader for UIC/CPM XML responses.
 *
 * Responses wrap payloads in `<UIC>`/`<CPM>` with a `<response result="ok">`
 * block; values may be plain text or CDATA. Rather than bind to an exact schema
 * (which varies subtly per command and firmware), this collects the text of
 * every leaf element into a flat, lower-cased map and exposes typed accessors.
 * The `result` attribute of `<response>` is captured separately.
 */
class WamResponse private constructor(
    val result: String?,
    private val values: Map<String, String>,
) {
    val isOk: Boolean get() = result.equals("ok", ignoreCase = true)

    fun str(tag: String): String? = values[tag.lowercase()]?.takeIf { it.isNotBlank() }
    fun int(tag: String): Int? = str(tag)?.toIntOrNull()
    fun bool(tag: String): Boolean? = when (str(tag)?.lowercase()) {
        "on", "true", "1" -> true
        "off", "false", "0" -> false
        else -> null
    }

    companion object {
        fun parse(xml: String): WamResponse {
            val values = HashMap<String, String>()
            var result: String? = null
            try {
                val parser = Xml.newPullParser()
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                parser.setInput(StringReader(xml))

                var currentTag: String? = null
                val text = StringBuilder()
                var event = parser.eventType
                while (event != XmlPullParser.END_DOCUMENT) {
                    when (event) {
                        XmlPullParser.START_TAG -> {
                            currentTag = parser.name?.lowercase()
                            text.setLength(0)
                            if (currentTag == "response") {
                                result = parser.getAttributeValue(null, "result")
                            }
                        }
                        XmlPullParser.TEXT, XmlPullParser.CDSECT -> {
                            if (currentTag != null) text.append(parser.text)
                        }
                        XmlPullParser.END_TAG -> {
                            val name = parser.name?.lowercase()
                            if (name != null && name == currentTag) {
                                val value = text.toString().trim()
                                if (value.isNotEmpty()) values[name] = value
                            }
                            currentTag = null
                            text.setLength(0)
                        }
                    }
                    event = parser.next()
                }
            } catch (_: Exception) {
                // Malformed / partial payloads are treated as "no usable values".
            }
            return WamResponse(result, values)
        }
    }
}
