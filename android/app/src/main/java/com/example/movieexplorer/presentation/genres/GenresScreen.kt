package com.example.movieexplorer.presentation.genres

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.movieexplorer.domain.Genre
import com.example.movieexplorer.presentation.MoviesUiState

@Composable
fun GenresScreen(state: MoviesUiState, onSelect: (Genre) -> Unit, onRetry: () -> Unit) {
    val minimumCell = if (LocalDensity.current.fontScale >= 1.3f) 200.dp else 150.dp
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Find your next favorite", style = MaterialTheme.typography.headlineSmall)
        Text("Explore films by genre", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (state.loading) CircularProgressIndicator()
        state.error?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.error)
            TextButton(onClick = onRetry) { Text("Retry") }
        }
        if (state.genres.isEmpty() && !state.loading && state.error == null) Text("No genres available")
        LazyVerticalGrid(columns = GridCells.Adaptive(minimumCell)) {
            items(state.genres, key = Genre::id) { genre ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp)
                        .clickable { onSelect(genre) },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .18f)).padding(20.dp)) {
                        Text(genre.name, style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(14.dp))
                        Text(
                            "BROWSE FILMS  ↗",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0E18)
@Composable
private fun GenresScreenPreview() {
    MaterialTheme {
        GenresScreen(
            MoviesUiState(
                genres = listOf(
                    Genre(28, "Action"),
                    Genre(12, "Adventure"),
                    Genre(16, "Animation")
                ), genresStatus = com.example.movieexplorer.presentation.LoadStatus.Success
            ),
            onSelect = {}, onRetry = {}
        )
    }
}
