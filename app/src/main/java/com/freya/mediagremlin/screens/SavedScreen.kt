package com.freya.mediagremlin.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.unit.dp
import com.freya.mediagremlin.components.PostCard
import com.freya.mediagremlin.data.Post
import androidx.compose.material3.MaterialTheme

@Composable
fun SavedScreen(
    posts: List<Post>,
    onSaveToggle: (Post) -> Unit
) {
    if (posts.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Nothing saved yet", style = MaterialTheme.typography.bodyMedium)
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(posts, key = { it.id }) { post ->
                PostCard(
                    post = post,
                    onSaveToggle = { onSaveToggle(post) }
                )
            }
        }
    }
}