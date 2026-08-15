package com.k650.remote.data

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader

/**
 * Lenient reader for UIC/CPM XML responses.
 *
 * Responses wrap payloads in `<UIC>`/`<CPM>` with a `<response result="ok">`
 * block; values may be plain text or CDATA. Scalars are read from a flat
 * lower-cased map ([str]/[int]/[bool]); repeated records (service lists, browse
 * menus, presets) are read with [items].
 */
class WamResponse private constructor(
    val result: String?,
    private val values: Map<String, String>,
    private val raw: String,
) {
    val isOk: Boolean get() = result.equals("ok", ignoreCase = true)

    fun str(tag: String): String? = values[tag.lowercase()]?.takeIf { it.isNotBlank() }
    fun int(tag: String): Int? = str(tag)?.toIntOrNull()
    fun bool(tag: String): Boolean? = when (str(tag)?.lowercase()) {
        "on", "true", "1" -> true
        "off", "false", "0" -> false
        else -> null
    }

    /**
     * Every element named [itemTag] as a record: its attributes keyed `@name`
     * plus its direct leaf children keyed by child tag (all lower-cased).
     */
    fun items(itemTag: String): List<Map<String, String>> {
        val target = itemTag.lowercase()
        val out = ArrayList<Map<String, String>>()
        try {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(StringReader(raw))

            var record: HashMap<String, String>? = null
            var childTag: String? = null
            val text = StringBuilder()
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                when (event) {
                    XmlPullParser.START_TAG -> {
                        val name = parser.name?.lowercase()
                        if (record == null && name == target) {
                            record = HashMap()
                            for (i in 0 until parser.attributeCount) {
                                record["@" + parser.getAttributeName(i).lowercase()] =
                                    parser.getAttributeValue(i)
                            }
                            childTag = null
                        } else if (record != null && name != null) {
                            childTag = name
                            text.setLength(0)
                        }
                    }
                    XmlPullParser.TEXT, XmlPullParser.CDSECT -> {
                        if (childTag != null) text.append(parser.text)
                    }
                    XmlPullParser.END_TAG -> {
                        val name = parser.name?.lowercase()
                        if (record != null && name == target) {
                            out.add(record)
                            record = null
                            childTag = null
                        } else if (record != null && name == childTag) {
                            val v = text.toString().trim()
                            if (v.isNotEmpty()) record[name] = v
                            childTag = null
                            text.setLength(0)
                        }
                    }
                }
                event = parser.next()
            }
        } catch (_: Exception) {
        }
        return out
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
            }
            return WamResponse(result, values, xml)
        }
    }
}
