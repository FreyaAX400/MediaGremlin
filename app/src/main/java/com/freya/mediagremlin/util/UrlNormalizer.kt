package com.freya.mediagremlin.util

import android.net.Uri

object UrlNormalizer {
    private val TRACKING_PARAMS = setOf(
        "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content",
        "fbclid", "gclid", "ref", "source", "mc_cid", "mc_eid", "_ga",
        "si", "s", "igshid"
    )

    fun normalize(raw: String): String {
        return try {
            val uri = Uri.parse(raw)
            val host = uri.host?.removePrefix("www.")?.lowercase() ?: return raw.lowercase()
            val scheme = uri.scheme?.lowercase() ?: "https"
            val path = uri.path?.trimEnd('/') ?: ""

            val cleanQuery = uri.queryParameterNames
                .filter { it !in TRACKING_PARAMS }
                .sorted()
                .mapNotNull { key ->
                    uri.getQueryParameter(key)?.let { "$key=$it" }
                }
                .joinToString("&")

            buildString {
                append("$scheme://$host$path")
                if (cleanQuery.isNotEmpty()) append("?$cleanQuery")
            }
        } catch (e: Exception) {
            raw.lowercase().trimEnd('/')
        }
    }
}