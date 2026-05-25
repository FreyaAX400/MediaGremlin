package com.freya.mediagremlin.sources

import com.freya.mediagremlin.data.Post
import com.freya.mediagremlin.network.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

object HNSource {
    private const val BASE = "https://hacker-news.firebaseio.com/v0"
    private const val LIMIT = 30

    suspend fun fetch(): List<Post> = withContext(Dispatchers.IO) {
        val ids = fetchTopIds().take(LIMIT)
        ids.map { id -> async { fetchStory(id) } }
            .awaitAll()
            .filterNotNull()
    }

    private fun fetchTopIds(): List<Long> {
        val request = Request.Builder().url("$BASE/topstories.json").build()
        val body = HttpClient.client.newCall(request).execute().body?.string() ?: return emptyList()
        val array = JSONArray(body)
        return (0 until array.length()).map { array.getLong(it) }
    }

    private fun fetchStory(id: Long): Post? {
        val request = Request.Builder().url("$BASE/item/$id.json").build()
        val body = HttpClient.client.newCall(request).execute().body?.string() ?: return null
        val json = JSONObject(body)
        if (json.optString("type") != "story") return null

        val externalUrl = json.optString("url")
        val isTextOnly = externalUrl.isBlank()

        return Post(
            id = "hn:$id",
            title = json.optString("title", ""),
            source = json.optString("by", "unknown"),
            platform = "HN",
            previewText = json.optString("text").takeIf { it.isNotBlank() },
            previewImageUrl = null,
            url = if (isTextOnly) "https://news.ycombinator.com/item?id=$id" else externalUrl,
            publishedAt = json.optLong("time", 0) * 1000,
            isTextOnly = isTextOnly
        )
    }
}
