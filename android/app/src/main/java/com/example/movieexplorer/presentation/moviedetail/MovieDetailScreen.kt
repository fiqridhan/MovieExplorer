package com.example.movieexplorer.presentation.moviedetail

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import coil.compose.AsyncImage
import com.example.movieexplorer.domain.MovieDetails

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun MovieDetailScreen(
    detail: MovieDetails,
    onBack: () -> Unit,
    onReviews: () -> Unit,
    onTrailer: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TextButton(onClick = onBack) { Text("← Movies") }
        detail.backdrop?.let { backdrop ->
            AsyncImage(
                backdrop,
                detail.movie.title,
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val wide = maxWidth >= 600.dp
            if (wide) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Poster(detail, Modifier.width(200.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        MovieFacts(detail)
                        Overview(detail)
                        DetailActions(detail, onReviews, onTrailer, wide = true)
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Poster(detail, Modifier.width(132.dp))
                        Box(Modifier.weight(1f)) { MovieFacts(detail) }
                    }
                    Overview(detail)
                    DetailActions(detail, onReviews, onTrailer, wide = false)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Poster(detail: MovieDetails, modifier: Modifier) {
    Box(
        modifier
            .aspectRatio(2f / 3f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        if (detail.movie.poster == null) Text("No poster", Modifier.align(Alignment.Center))
        else AsyncImage(
            detail.movie.poster,
            detail.movie.title,
            Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun MovieFacts(detail: MovieDetails) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        detail.movie.rating.takeIf { it > 0 }?.let { rating ->
            Text(
                "★ ${"%.1f".format(rating)} / 10",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        detail.voteCount?.takeIf { it > 0 }
            ?.let { Text("$it votes", style = MaterialTheme.typography.bodyMedium) }
        listOfNotNull(
            detail.releaseDate?.let { "Released $it" },
            detail.runtime?.let { "$it min" },
            detail.status
        ).forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
        if (detail.genres.isNotEmpty()) Text(
            detail.genres.joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium
        )
        detail.language?.let {
            Text(
                "Original language: ${it.uppercase()}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun Overview(detail: MovieDetails) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Overview", style = MaterialTheme.typography.titleLarge)
        Text(
            detail.movie.overview.ifBlank { "No synopsis available." },
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun DetailActions(
    detail: MovieDetails,
    onReviews: () -> Unit,
    onTrailer: () -> Unit,
    wide: Boolean
) {
    if (wide) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onReviews) { Text("Reviews") }
            TrailerAction(detail, onTrailer)
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onReviews, modifier = Modifier.fillMaxWidth()) { Text("Reviews") }
            TrailerAction(detail, onTrailer, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun TrailerAction(
    detail: MovieDetails,
    onTrailer: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (detail.trailer != null) OutlinedButton(
        onClick = onTrailer,
        modifier = modifier
    ) { Text("Watch trailer") }
    else Text(
        "Trailer unavailable",
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0E18)
@Composable
private fun MovieDetailScreenPreview() {
    val movie = com.example.movieexplorer.domain.Movie(
        1,
        "Sample film",
        "A sample synopsis for previewing the detail layout.",
        null,
        8.1
    )
    MaterialTheme {
        MovieDetailScreen(
            MovieDetails(
                movie,
                120,
                "2025-01-01",
                "Released",
                null
            ), {}, {}, {})
    }
}
