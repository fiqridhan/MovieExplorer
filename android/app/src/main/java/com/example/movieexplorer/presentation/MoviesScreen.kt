package com.example.movieexplorer.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.movieexplorer.presentation.discover.DiscoverScreen
import com.example.movieexplorer.presentation.genres.GenresScreen
import com.example.movieexplorer.presentation.moviedetail.MovieDetailScreen
import com.example.movieexplorer.presentation.reviews.ReviewsScreen
import com.example.movieexplorer.presentation.trailer.TrailerScreen

@Composable
fun MovieApp(vm: MoviesViewModel = hiltViewModel()) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val detail = state.selected
    val movieGridState = rememberLazyGridState()
    LaunchedEffect(state.genre?.id) {
        if (state.genre != null) movieGridState.scrollToItem(0)
    }
    BackHandler(enabled = detail != null || state.genre != null) {
        if (detail != null) vm.close() else vm.clearGenre()
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            detail?.movie?.title ?: state.genre?.name ?: "MovieExplorer",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 12.dp)
        )
        when {
            detail != null && state.showingTrailer -> TrailerScreen(detail.trailer, vm::close)
            detail != null && state.showingReviews -> ReviewsScreen(
                state,
                vm::close,
                vm::loadMoreReviews,
                vm::retry
            )

            detail != null -> MovieDetailScreen(detail, vm::close, vm::showReviews, vm::showTrailer)
            state.genre == null -> GenresScreen(state, vm::choose, vm::retry)
            else -> DiscoverScreen(
                state,
                movieGridState,
                vm::clearGenre,
                vm::choose,
                vm::open,
                vm::loadMore,
                vm::refreshMovies,
                vm::retry
            )
        }
    }
}
