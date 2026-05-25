package com.freya.mediagremlin.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.freya.mediagremlin.data.AppDatabase
import com.freya.mediagremlin.data.Post
import com.freya.mediagremlin.sources.HNSource
import com.freya.mediagremlin.sources.RSSSource
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PostViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val dao = db.postDao()
    private val sourceDao = db.sourceDao()

    val posts: StateFlow<List<Post>> = dao.getAllPosts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedPosts: StateFlow<List<Post>> = dao.getSavedPosts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    private val _lastFetchTime = MutableStateFlow<Long?>(null)
    val lastFetchTime: StateFlow<Long?> = _lastFetchTime

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val sources = sourceDao.getAllSources().first().filter { it.enabled }
                val posts = sources.map { source ->
                    async {
                        when (source.type) {
                            "HN" -> HNSource.fetch()
                            "RSS" -> RSSSource.fetch(source.id, source.name, source.url)
                            else -> emptyList()
                        }
                    }
                }.awaitAll().flatten()
                dao.insertAll(posts) // unique index on urlNormalized silently drops duplicates
                _lastFetchTime.value = System.currentTimeMillis()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun toggleSave(post: Post) {
        viewModelScope.launch {
            dao.setSaved(post.id, !post.saved)
        }
    }
}