package com.example.movieexplorer.presentation

import com.example.movieexplorer.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MoviesViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)
    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun selectedGenreLoadsPagesAndStopsAtLastPage() = runTest(dispatcher) {
        val repository = FakeMoviesRepository()
        val viewModel = MoviesViewModel(repository)
        advanceUntilIdle()
        viewModel.choose(Genre(28, "Action"))
        advanceUntilIdle()
        viewModel.loadMore()
        advanceUntilIdle()
        viewModel.loadMore()
        advanceUntilIdle()

        assertEquals(listOf(1, 2), repository.moviePages)
        assertEquals(listOf(7, 8), viewModel.movies.map(Movie::id))
        assertFalse(viewModel.loading)
    }

    @Test
    fun failedPageRetriesWithoutLosingExistingMovies() = runTest(dispatcher) {
        val repository = FakeMoviesRepository().apply { failPageTwoOnce = true }
        val viewModel = MoviesViewModel(repository)
        advanceUntilIdle()
        viewModel.choose(Genre(28, "Action"))
        advanceUntilIdle()
        viewModel.loadMore()
        advanceUntilIdle()
        assertEquals(listOf(7), viewModel.movies.map(Movie::id))
        assertTrue(viewModel.error != null)
        viewModel.retry()
        advanceUntilIdle()
        assertEquals(listOf(1, 2, 2), repository.moviePages)
        assertEquals(listOf(7, 8), viewModel.movies.map(Movie::id))
        assertEquals(null, viewModel.error)
    }

    @Test
    fun failedRefreshKeepsPreviouslyLoadedMovies() = runTest(dispatcher) {
        val repository = FakeMoviesRepository()
        val viewModel = MoviesViewModel(repository)
        advanceUntilIdle()
        viewModel.choose(Genre(28, "Action"))
        advanceUntilIdle()
        repository.failPageOneOnce = true
        viewModel.refreshMovies()
        advanceUntilIdle()

        assertEquals(listOf(7), viewModel.movies.map(Movie::id))
        assertFalse(viewModel.refreshing)
        assertTrue(viewModel.error != null)
    }

    @Test
    fun genreFailureLeavesRetryableErrorState() = runTest(dispatcher) {
        val repository = FakeMoviesRepository().apply { failGenresOnce = true }
        val viewModel = MoviesViewModel(repository)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.error != null)
        assertFalse(viewModel.uiState.value.loading)
        viewModel.retry()
        advanceUntilIdle()
        assertEquals("Action", viewModel.uiState.value.genres.single().name)
        assertEquals(null, viewModel.uiState.value.error)
    }

    @Test
    fun rapidPageRequestsAreDeduplicated() = runTest(dispatcher) {
        val repository = FakeMoviesRepository().apply { duplicateOnSecondPage = true }
        val viewModel = MoviesViewModel(repository)
        advanceUntilIdle()
        viewModel.choose(Genre(28, "Action"))
        advanceUntilIdle()
        viewModel.loadMore()
        viewModel.loadMore()
        advanceUntilIdle()
        assertEquals(listOf(1, 2), repository.moviePages)
        assertEquals(listOf(7), viewModel.movies.map(Movie::id))
    }

    @Test
    fun reviewPaginationStopsAtFinalPageAndDeduplicates() = runTest(dispatcher) {
        val repository = FakeMoviesRepository()
        val viewModel = MoviesViewModel(repository)
        advanceUntilIdle()
        viewModel.choose(Genre(28, "Action"))
        advanceUntilIdle()
        viewModel.open(viewModel.movies.first())
        advanceUntilIdle()
        viewModel.showReviews()
        advanceUntilIdle()
        viewModel.loadMoreReviews()
        advanceUntilIdle()
        viewModel.loadMoreReviews()
        advanceUntilIdle()
        assertEquals(listOf(1, 2), repository.reviewPages)
        assertEquals(listOf("r1"), viewModel.reviews.map(Review::id))
    }

    @Test
    fun missingTokenShowsConfigurationInstruction() = runTest(dispatcher) {
        val repository = FakeMoviesRepository().apply { missingToken = true }
        val viewModel = MoviesViewModel(repository)
        advanceUntilIdle()
        assertEquals("Set TMDB_ACCESS_TOKEN in local.properties.", viewModel.error)
    }

    @Test
    fun emptyGenresExposeEmptyState() = runTest(dispatcher) {
        val viewModel = MoviesViewModel(FakeMoviesRepository().apply { emptyGenres = true })
        advanceUntilIdle()
        assertEquals(LoadStatus.Empty, viewModel.uiState.value.genresStatus)
        assertFalse(viewModel.loading)
    }

    @Test
    fun emptyMoviePageExposesEmptyState() = runTest(dispatcher) {
        val viewModel = MoviesViewModel(FakeMoviesRepository().apply { emptyMovies = true })
        advanceUntilIdle()
        viewModel.choose(Genre(28, "Action"))
        advanceUntilIdle()
        assertEquals(LoadStatus.Empty, viewModel.uiState.value.moviesStatus)
        assertFalse(viewModel.loading)
    }

    @Test
    fun failedReviewPageRetriesWithoutLosingReviews() = runTest(dispatcher) {
        val repository = FakeMoviesRepository().apply { failReviewPageTwoOnce = true }
        val viewModel = MoviesViewModel(repository)
        advanceUntilIdle()
        viewModel.choose(Genre(28, "Action"))
        advanceUntilIdle()
        viewModel.open(viewModel.movies.first())
        advanceUntilIdle()
        viewModel.showReviews()
        advanceUntilIdle()
        viewModel.loadMoreReviews()
        advanceUntilIdle()
        assertEquals(LoadStatus.PaginationError, viewModel.uiState.value.reviewsStatus)
        assertEquals(listOf("r1"), viewModel.reviews.map(Review::id))
        viewModel.retry()
        advanceUntilIdle()
        assertEquals(listOf(1, 2, 2), repository.reviewPages)
        assertEquals(LoadStatus.Success, viewModel.uiState.value.reviewsStatus)
    }

    private class FakeMoviesRepository : MoviesRepository {
        val moviePages = mutableListOf<Int>()
        var failPageTwoOnce = false
        var failPageOneOnce = false
        var failGenresOnce = false
        var duplicateOnSecondPage = false
        var missingToken = false
        var emptyGenres = false
        var emptyMovies = false
        var failReviewPageTwoOnce = false
        val reviewPages = mutableListOf<Int>()

        override suspend fun genres(): List<Genre> {
            if (missingToken) throw IllegalArgumentException("Set TMDB_ACCESS_TOKEN in local.properties.")
            if (failGenresOnce) {
                failGenresOnce = false; error("Temporary failure")
            }
            return if (emptyGenres) emptyList() else listOf(Genre(28, "Action"))
        }

        override suspend fun movies(genreId: Int, page: Int): Page<Movie> {
            moviePages += page
            if (page == 1 && failPageOneOnce) {
                failPageOneOnce = false
                error("Temporary failure")
            }
            if (page == 2 && failPageTwoOnce) {
                failPageTwoOnce = false
                error("Temporary failure")
            }
            val movie =
                Movie(if (page == 1 || duplicateOnSecondPage) 7 else 8, "Film", "Story", null, 7.0)
            return Page(
                if (emptyMovies) emptyList() else listOf(movie),
                page,
                if (emptyMovies) 1 else 2
            )
        }

        override suspend fun details(movie: Movie) = MovieDetails(movie, null, null, null, null)
        override suspend fun reviews(movieId: Int, page: Int): Page<Review> {
            reviewPages += page
            if (page == 2 && failReviewPageTwoOnce) {
                failReviewPageTwoOnce = false
                error("Temporary failure")
            }
            return Page(listOf(Review("r1", "Viewer", "Good movie")), page, 2)
        }
    }
}
