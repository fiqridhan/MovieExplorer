package com.example.movieexplorer.presentation.discover

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.movieexplorer.domain.Genre
import com.example.movieexplorer.domain.Movie
import com.example.movieexplorer.presentation.MoviesUiState
import com.example.movieexplorer.presentation.LoadStatus

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    state: MoviesUiState,
    gridState: LazyGridState,
    onBack: () -> Unit,
    onSelectGenre: (Genre) -> Unit,
    onSelectMovie: (Movie) -> Unit,
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit
) {
    val largeText = LocalDensity.current.fontScale >= 1.3f
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TextButton(onClick = onBack) { Text("← All genres") }
        Text("Popular in ${state.genre?.name.orEmpty()}", style = MaterialTheme.typography.headlineSmall)
            Row(Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())) {
            state.genres.forEach { genre ->
                TextButton(onClick = { onSelectGenre(genre) }) {
                    Text(
                        genre.name
                    )
                }
            }
        }
        state.error?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.error)
            TextButton(onClick = onRetry) { Text("Retry") }
        }
        if (state.detailStatus == LoadStatus.Loading) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }
        if (state.movies.isEmpty() && !state.loading && state.error == null) Text("No movies found for this genre")
        LaunchedEffect(gridState, state.movies.size) {
            snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
                .collect { if (it >= state.movies.size - 3) onLoadMore() }
        }
        PullToRefreshBox(isRefreshing = state.refreshing, onRefresh = onRefresh) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val columns =
                    if (maxWidth >= 700.dp) 4 else if (maxWidth >= 480.dp) 3 else if (largeText) 1 else 2
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    state = gridState,
                    modifier = Modifier.testTag("movieGrid")
                ) {
                    if (state.loading && state.movies.isEmpty()) {
                        items(6) { PosterSkeleton() }
                    }
                    items(state.movies, key = Movie::id) { movie ->
                        MovieCard(movie, onSelectMovie)
                    }
                    if (state.loading && state.movies.isNotEmpty() && !state.refreshing) {
                        item { CircularProgressIndicator(Modifier.padding(16.dp)) }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0E18)
@Composable
private fun DiscoverScreenPreview() {
    MaterialTheme {
        DiscoverScreen(
            MoviesUiState(
                genre = Genre(28, "Action"),
                genres = listOf(Genre(28, "Action")),
                movies = listOf(Movie(1, "Sample film", "", null, 7.8, "2025-01-01"))
            ),
            androidx.compose.foundation.lazy.grid.rememberLazyGridState(), {}, {}, {}, {}, {}, {}
        )
    }
}

@Composable
private fun PosterSkeleton() {
    Card(
        Modifier
            .fillMaxWidth()
            .padding(6.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Spacer(Modifier.height(38.dp))
    }
}

@Composable
private fun MovieCard(movie: Movie, onSelect: (Movie) -> Unit) {
    Card(
        Modifier
            .fillMaxWidth()
            .padding(6.dp)
            .clickable { onSelect(movie) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (movie.poster == null) Text("Poster unavailable", Modifier.padding(16.dp))
            else AsyncImage(movie.poster, movie.title, Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
        }
        Text(
            movie.title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
        val metadata = listOfNotNull(
            movie.releaseDate?.take(4),
            movie.rating.takeIf { it > 0 }?.let { "★ ${"%.1f".format(it)}" })
        Text(
            metadata.joinToString(" · "),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
