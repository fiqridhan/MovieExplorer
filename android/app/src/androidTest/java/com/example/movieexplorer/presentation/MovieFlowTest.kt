package com.example.movieexplorer.presentation

import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performClick
import com.example.movieexplorer.domain.*
import org.junit.Rule
import org.junit.Test

class MovieFlowTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun genreToMovieDetailAndReviews() {
        val viewModel = MoviesViewModel(FakeRepository())
        compose.activity.runOnUiThread {
            compose.activity.setContent { MaterialTheme { MovieApp(viewModel) } }
        }

        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Action").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Action").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Film").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Film").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Story").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Story").assertExists()
        compose.onNodeWithText("Reviews").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Good movie").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Good movie").assertExists()
    }

    @Test
    fun returningFromDetailKeepsMovieScrollPosition() {
        val viewModel = MoviesViewModel(FakeRepository(manyMovies = true))
        compose.activity.runOnUiThread {
            compose.activity.setContent { MaterialTheme { MovieApp(viewModel) } }
        }
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Action").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Action").performClick()
        compose.waitUntil(5_000) { viewModel.movies.size == 30 }
        compose.onNodeWithTag("movieGrid").performScrollToIndex(18)
        compose.onNodeWithText("Film 19").performClick()
        compose.onNodeWithText("← Movies").performClick()
        compose.onNodeWithText("Film 19").assertIsDisplayed()
    }

    @Test
    fun emptyGenresShowAnEmptyState() {
        val viewModel = MoviesViewModel(FakeRepository(emptyGenres = true))
        compose.activity.runOnUiThread {
            compose.activity.setContent { MaterialTheme { MovieApp(viewModel) } }
        }
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("No genres available").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("No genres available").assertIsDisplayed()
    }

    @Test
    fun genreFailureCanBeRetried() {
        val viewModel = MoviesViewModel(FakeRepository(failGenresOnce = true))
        compose.activity.runOnUiThread {
            compose.activity.setContent { MaterialTheme { MovieApp(viewModel) } }
        }
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Retry").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Retry").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Action").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Action").assertIsDisplayed()
    }

    private class FakeRepository(
        private val manyMovies: Boolean = false,
        private val emptyGenres: Boolean = false,
        private var failGenresOnce: Boolean = false
    ) : MoviesRepository {
        private val movie = Movie(7, "Film", "Story", null, 8.0)
        override suspend fun genres(): List<Genre> {
            if (failGenresOnce) {
                failGenresOnce = false
                error("Temporary failure")
            }
            return if (emptyGenres) emptyList() else listOf(Genre(28, "Action"))
        }

        override suspend fun movies(genreId: Int, page: Int) = Page(
            if (manyMovies) (1..30).map { Movie(it, "Film $it", "Story", null, 8.0) } else listOf(
                movie
            ), 1, 1
        )

        override suspend fun details(movie: Movie) =
            MovieDetails(movie, 120, "2026-01-01", "Released", null)

        override suspend fun reviews(movieId: Int, page: Int) =
            Page(listOf(Review("r1", "Viewer", "Good movie")), 1, 1)
    }
}
