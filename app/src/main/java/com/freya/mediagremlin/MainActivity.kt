package com.freya.mediagremlin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.lifecycle.viewmodel.compose.viewModel
import com.freya.mediagremlin.screens.PostsScreen
import com.freya.mediagremlin.screens.SavedScreen
import com.freya.mediagremlin.screens.SettingsScreen
import com.freya.mediagremlin.ui.theme.MediaGremlinTheme
import com.freya.mediagremlin.viewmodel.PostViewModel
import com.freya.mediagremlin.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MediaGremlinTheme {
                MediaGremlinApp()
            }
        }
    }
}

@PreviewScreenSizes
@Composable
fun MediaGremlinApp() {
    val postViewModel: PostViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()
    val posts by postViewModel.posts.collectAsState()
    val savedPosts by postViewModel.savedPosts.collectAsState()
    val isRefreshing by postViewModel.isRefreshing.collectAsState()
    val lastFetchTime by postViewModel.lastFetchTime.collectAsState()
    val sources by settingsViewModel.sources.collectAsState()
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }

    LaunchedEffect(Unit) {
        postViewModel.refresh()
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            painterResource(it.icon),
                            contentDescription = it.label
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        when (currentDestination) {
            AppDestinations.HOME -> PostsScreen(
                posts = posts,
                isRefreshing = isRefreshing,
                onRefresh = postViewModel::refresh,
                onSaveToggle = postViewModel::toggleSave
            )
            AppDestinations.SAVED -> SavedScreen(
                posts = savedPosts,
                onSaveToggle = postViewModel::toggleSave
            )
            AppDestinations.SETTINGS -> SettingsScreen(
                sources = sources,
                lastFetchTime = lastFetchTime,
                onToggleSource = settingsViewModel::toggleSource,
                onClearCache = settingsViewModel::clearCache
            )
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: Int,
) {
    SAVED("Saved", R.drawable.ic_bookmark),
    HOME("Posts", R.drawable.ic_cards),
    SETTINGS("Settings", R.drawable.ic_settings),
}

