package com.freya.mediagremlin.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.freya.mediagremlin.data.AppDatabase
import com.freya.mediagremlin.data.Post
import com.freya.mediagremlin.sources.HNSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PostViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getInstance(application).postDao()

    val posts: StateFlow<List<Post>> = dao.getAllPosts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedPosts: StateFlow<List<Post>> = dao.getSavedPosts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _lastFetchTime = MutableStateFlow<Long?>(null)
    val lastFetchTime: StateFlow<Long?> = _lastFetchTime
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing
    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val posts = HNSource.fetch()
                dao.insertAll(posts)
                _lastFetchTime.value = System.currentTimeMillis()
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

    init {
        viewModelScope.launch {
        }
    }
}