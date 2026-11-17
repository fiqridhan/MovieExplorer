package com.example.movieexplorer.presentation.reviews

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import coil.compose.AsyncImage
import com.example.movieexplorer.domain.Review
import com.example.movieexplorer.presentation.MoviesUiState

@Composable
fun ReviewsScreen(
    state: MoviesUiState,
    onBack: () -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit
) {
    val listState = rememberLazyListState()
    Column {
        TextButton(onClick = onBack) { Text("← Movie details") }
        state.error?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.error)
            TextButton(onClick = onRetry) { Text("Retry") }
        }
        if (state.reviews.isEmpty() && !state.loading && state.error == null) Text("No reviews available")
        LaunchedEffect(listState, state.reviews.size) {
            snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
                .collect { if (it >= state.reviews.size - 3) onLoadMore() }
        }
        LazyColumn(state = listState) {
            items(state.reviews, key = Review::id) { review -> ReviewCard(review) }
            if (state.loading) item { CircularProgressIndicator(Modifier.padding(16.dp)) }
        }
    }
}

@Composable
private fun ReviewCard(review: Review) {
    Column(Modifier.padding(vertical = 12.dp)) {
        Row {
            if (review.avatar != null) AsyncImage(review.avatar, null, Modifier.size(40.dp))
            else Text(review.author.take(1).ifBlank { "?" }, Modifier.size(40.dp))
            Text(
                review.author,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        review.rating?.let { Text("★ $it") }
        review.createdAt?.let { Text(it.take(10)) }
        Text(review.content)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0E18)
@Composable
private fun ReviewsScreenPreview() {
    MaterialTheme {
        ReviewsScreen(
            MoviesUiState(
                reviews = listOf(
                    Review(
                        "sample",
                        "Movie fan",
                        "A sample review shown in the preview."
                    )
                )
            ), {}, {}, {})
    }
}
