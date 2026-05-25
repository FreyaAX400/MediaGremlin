package com.freya.mediagremlin.sources

import android.util.Xml
import com.freya.mediagremlin.data.Post
import com.freya.mediagremlin.network.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Locale
import com.freya.mediagremlin.util.UrlNormalizer

object RSSSource {

    suspend fun fetch(sourceId: String, sourceName: String, url: String): List<Post> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "MediaGremlin/1.0")
                    .build()
                val body = HttpClient.client.newCall(request).execute().body.string()
                parse(body, sourceId, sourceName)
            } catch (_: Exception) {
                emptyList()
            }
        }

    private fun parse(xml: String, sourceId: String, sourceName: String): List<Post> {
        return try {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
            parser.setInput(StringReader(xml))
            while (parser.eventType != XmlPullParser.START_TAG) parser.next()
            when (parser.name) {
                "rss", "rdf:RDF", "RDF" -> parseRSS(parser, sourceId, sourceName)
                "feed" -> parseAtom(parser, sourceId, sourceName)
                else -> emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    // --- RSS 2.0 ---

    private fun parseRSS(parser: XmlPullParser, sourceId: String, sourceName: String): List<Post> {
        val posts = mutableListOf<Post>()
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG && parser.name == "item") {
                parseRSSItem(parser, sourceId, sourceName)?.let { posts.add(it) }
            }
            parser.next()
        }
        return posts
    }

    private fun parseRSSItem(parser: XmlPullParser, sourceId: String, sourceName: String): Post? {
        var title = ""
        var link = ""
        var description: String? = null
        var imageUrl: String? = null
        var pubDate: Long = System.currentTimeMillis()
        parser.next()
        while (!(parser.eventType == XmlPullParser.END_TAG && parser.name == "item")) {
            if (parser.eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "title" -> title = parser.nextText().trim()
                    "link" -> link = parser.nextText().trim()
                    "description" -> {
                        description = stripHtml(parser.nextText().trim()).takeIf { it.isNotBlank() }
                    }

                    "pubDate" -> pubDate = parseRSSDate(parser.nextText().trim())
                    "creator" -> parser.nextText() // consume dc:creator
                    "content" if imageUrl == null -> {
                        val medium = parser.getAttributeValue(null, "medium")
                        val type = parser.getAttributeValue(null, "type") ?: ""
                        val url = parser.getAttributeValue(null, "url") ?: ""
                        if ((medium == "image" || type.startsWith("image/")) && url.isNotBlank()) imageUrl = url
                    }

                    "thumbnail" if imageUrl == null -> {
                        val url = parser.getAttributeValue(null, "url") ?: ""
                        if (url.isNotBlank()) imageUrl = url
                    }

                    "enclosure" if imageUrl == null -> {
                        val type = parser.getAttributeValue(null, "type") ?: ""
                        val url = parser.getAttributeValue(null, "url") ?: ""
                        if (type.startsWith("image/") && url.isNotBlank()) imageUrl = url
                    }
                }
            }
            parser.next()
        }
        if (link.isBlank()) return null
        val normalizedLink = UrlNormalizer.normalize(link)
        return Post(
            id = "rss:$sourceId:${normalizedLink.hashCode()}",
            title = title,
            source = sourceName,
            platform = "RSS",
            previewText = description?.take(300),
            previewImageUrl = imageUrl,
            url = link,
            urlNormalized = normalizedLink,
            publishedAt = pubDate,
            isTextOnly = false
        )
    }

    // --- Atom ---

    private fun parseAtom(parser: XmlPullParser, sourceId: String, sourceName: String): List<Post> {
        val posts = mutableListOf<Post>()
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG && parser.name == "entry") {
                parseAtomEntry(parser, sourceId, sourceName)?.let { posts.add(it) }
            }
            parser.next()
        }
        return posts
    }

    private fun parseAtomEntry(parser: XmlPullParser, sourceId: String, sourceName: String): Post? {
        var title = ""
        var link = ""
        var summary: String? = null
        var imageUrl: String? = null
        var published: Long = System.currentTimeMillis()
        parser.next()
        while (!(parser.eventType == XmlPullParser.END_TAG && parser.name == "entry")) {
            if (parser.eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "title" -> title = parser.nextText().trim()
                    "link" -> {
                        val rel = parser.getAttributeValue(null, "rel") ?: "alternate"
                        val href = parser.getAttributeValue(null, "href") ?: ""
                        if (rel == "alternate" && href.isNotBlank()) link = href
                    }
                    "summary" -> {
                        summary = stripHtml(parser.nextText().trim()).takeIf { it.isNotBlank() }
                    }
                    "content" -> {
                        val url = parser.getAttributeValue(null, "url")
                        if (url != null && url.isNotBlank()) {
                            if (imageUrl == null) imageUrl = url
                        } else {
                            summary = stripHtml(parser.nextText().trim()).takeIf { it.isNotBlank() }
                        }
                    }
                    "thumbnail" if imageUrl == null -> {
                        val url = parser.getAttributeValue(null, "url") ?: ""
                        if (url.isNotBlank()) imageUrl = url
                    }
                    "published" -> published = parseISODate(parser.nextText().trim())
                    "updated" -> {
                        if (published == 0L) published = parseISODate(parser.nextText().trim())
                    }
                }
            }
            parser.next()
        }
        if (link.isBlank()) return null
        val normalizedLink = UrlNormalizer.normalize(link)
        return Post(
            id = "rss:$sourceId:${normalizedLink.hashCode()}",
            title = title,
            source = sourceName,
            platform = "RSS",
            previewText = summary?.take(300),
            previewImageUrl = imageUrl,
            url = link,
            urlNormalized = normalizedLink,
            publishedAt = published,
            isTextOnly = false
        )
    }

    // --- Helpers ---

    private fun stripHtml(html: String) = html.replace(Regex("<[^>]+>"), "").trim()

    private fun parseRSSDate(date: String): Long {
        val formats = listOf(
            "EEE, dd MMM yyyy HH:mm:ss Z",
            "EEE, dd MMM yyyy HH:mm:ss z",
            "dd MMM yyyy HH:mm:ss Z"
        )
        for (format in formats) {
            try {
                return SimpleDateFormat(format, Locale.ENGLISH).parse(date)?.time
                    ?: continue
            } catch (_: Exception) { continue }
        }
        return System.currentTimeMillis()
    }

    private fun parseISODate(date: String): Long {
        return try {
            java.time.Instant.parse(date).toEpochMilli()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }
}