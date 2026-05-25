package com.freya.mediagremlin.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.freya.mediagremlin.data.AppDatabase
import com.freya.mediagremlin.data.Source
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val sourceDao = db.sourceDao()
    private val postDao = db.postDao()

    val sources: StateFlow<List<Source>> = sourceDao.getAllSources()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleSource(source: Source) {
        viewModelScope.launch {
            sourceDao.setEnabled(source.id, !source.enabled)
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            postDao.clearUnsaved()
        }
    }

    init {
        viewModelScope.launch {
            if (sourceDao.getAllSources().first().isEmpty()) {
                sourceDao.insertAll(
                    listOf(
                        Source(
                            id = "hn",
                            name = "Hacker News",
                            type = "HN",
                            url = "https://hacker-news.firebaseio.com/v0",
                            enabled = true
                        )
                    )
                )
            }
        }
    }
}