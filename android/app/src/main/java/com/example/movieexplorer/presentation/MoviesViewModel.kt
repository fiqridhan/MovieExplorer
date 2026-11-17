package com.example.movieexplorer.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movieexplorer.domain.*
import dagger.hilt.android.lifecycle.HiltViewModel
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class LoadStatus { Initial, Loading, Success, Empty, Error, Refreshing, LoadingMore, PaginationError }

data class MoviesUiState(
    val genres: List<Genre> = emptyList(),
    val movies: List<Movie> = emptyList(),
    val selected: MovieDetails? = null,
    val reviews: List<Review> = emptyList(),
    val showingReviews: Boolean = false,
    val showingTrailer: Boolean = false,
    val genre: Genre? = null,
    val genresStatus: LoadStatus = LoadStatus.Initial,
    val moviesStatus: LoadStatus = LoadStatus.Initial,
    val detailStatus: LoadStatus = LoadStatus.Initial,
    val reviewsStatus: LoadStatus = LoadStatus.Initial,
    val genresError: String? = null,
    val moviesError: String? = null,
    val detailError: String? = null,
    val reviewsError: String? = null
) {
    val status: LoadStatus
        get() = when {
            showingReviews -> reviewsStatus
            selected != null -> detailStatus
            genre != null -> moviesStatus
            else -> genresStatus
        }
    val error: String?
        get() = when {
            showingReviews -> reviewsError
            selected != null -> detailError
            genre != null -> detailError ?: moviesError
            else -> genresError
        }
    val loading: Boolean get() = status == LoadStatus.Loading || status == LoadStatus.LoadingMore
    val refreshing: Boolean get() = status == LoadStatus.Refreshing
}

@HiltViewModel
class MoviesViewModel @Inject constructor(repository: MoviesRepository) : ViewModel() {
    private val getGenres = GetGenres(repository)
    private val discoverMovies = DiscoverMovies(repository)
    private val getDetails = GetMovieDetails(repository)
    private val getReviews = GetMovieReviews(repository)
    private val _uiState = MutableStateFlow(MoviesUiState())
    val uiState = _uiState.asStateFlow()

    val movies get() = uiState.value.movies
    val reviews get() = uiState.value.reviews
    val loading get() = uiState.value.loading
    val refreshing get() = uiState.value.refreshing
    val error get() = uiState.value.error

    private var moviePage = 1
    private var reviewPage = 1
    private var hasMoviePages = true
    private var hasReviewPages = true
    private var request: Job? = null
    private var retryAction: (() -> Unit)? = null

    init {
        loadGenres()
    }

    fun loadGenres() {
        if (uiState.value.genresStatus == LoadStatus.Loading) return
        _uiState.update { it.copy(genresStatus = LoadStatus.Loading, genresError = null) }
        request = viewModelScope.launch {
            try {
                val genres = getGenres().distinctBy(Genre::id)
                _uiState.update {
                    it.copy(
                        genres = genres,
                        genresStatus = if (genres.isEmpty()) LoadStatus.Empty else LoadStatus.Success
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        genresStatus = LoadStatus.Error,
                        genresError = message(exception, "Could not load genres")
                    )
                }
            }
        }
    }

    fun choose(value: Genre?) {
        if (value == null || value.id == uiState.value.genre?.id) return
        cancelRequest()
        moviePage = 1
        hasMoviePages = true
        _uiState.update {
            it.copy(
                genre = value,
                movies = emptyList(),
                selected = null,
                moviesStatus = LoadStatus.Initial,
                moviesError = null,
                detailError = null
            )
        }
        loadMore()
    }

    fun clearGenre() {
        cancelRequest()
        _uiState.update {
            it.copy(
                genre = null,
                movies = emptyList(),
                selected = null,
                moviesStatus = LoadStatus.Initial,
                moviesError = null,
                detailError = null
            )
        }
    }

    fun refreshMovies() {
        if (uiState.value.genre == null) return
        cancelRequest()
        moviePage = 1
        hasMoviePages = true
        _uiState.update { it.copy(moviesStatus = LoadStatus.Refreshing, moviesError = null) }
        loadMore()
    }

    fun loadMore() {
        val state = uiState.value
        val genreId = state.genre?.id ?: return
        if (!hasMoviePages || state.moviesStatus in listOf(
                LoadStatus.Loading,
                LoadStatus.LoadingMore
            )
        ) return
        if (state.moviesStatus == LoadStatus.Refreshing && request?.isActive == true) return
        if (state.moviesStatus in listOf(LoadStatus.Error, LoadStatus.PaginationError)) return
        val refreshing = state.moviesStatus == LoadStatus.Refreshing
        _uiState.update {
            it.copy(
                moviesStatus = if (refreshing) LoadStatus.Refreshing else if (it.movies.isEmpty()) LoadStatus.Loading else LoadStatus.LoadingMore,
                moviesError = null
            )
        }
        retryAction = if (refreshing) ::refreshMovies else ::retryMoviePage
        request = viewModelScope.launch {
            try {
                val result = discoverMovies(genreId, moviePage)
                if (uiState.value.genre?.id != genreId) return@launch
                moviePage = result.number + 1
                hasMoviePages = result.number < result.totalPages
                _uiState.update {
                    val items =
                        ((if (refreshing) emptyList() else it.movies) + result.items).distinctBy(
                            Movie::id
                        )
                    it.copy(
                        movies = items,
                        moviesStatus = if (items.isEmpty()) LoadStatus.Empty else LoadStatus.Success
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        moviesStatus = if (it.movies.isEmpty()) LoadStatus.Error else LoadStatus.PaginationError,
                        moviesError = message(exception, "Could not load movies")
                    )
                }
            }
        }
    }

    private fun retryMoviePage() {
        _uiState.update { it.copy(moviesStatus = if (it.movies.isEmpty()) LoadStatus.Initial else LoadStatus.Success) }
        loadMore()
    }

    fun open(movie: Movie) {
        if (uiState.value.detailStatus == LoadStatus.Loading) return
        cancelRequest()
        _uiState.update { it.copy(detailStatus = LoadStatus.Loading, detailError = null) }
        retryAction = { open(movie) }
        request = viewModelScope.launch {
            try {
                val detail = getDetails(movie)
                _uiState.update { it.copy(selected = detail, detailStatus = LoadStatus.Success) }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        detailStatus = LoadStatus.Error,
                        detailError = message(exception, "Could not load movie details")
                    )
                }
            }
        }
    }

    fun showReviews() {
        val movieId = uiState.value.selected?.movie?.id ?: return
        cancelRequest()
        reviewPage = 1
        hasReviewPages = true
        _uiState.update {
            it.copy(
                showingReviews = true,
                reviews = emptyList(),
                reviewsStatus = LoadStatus.Initial,
                reviewsError = null
            )
        }
        loadMoreReviews(movieId)
    }

    fun loadMoreReviews() {
        val movieId = uiState.value.selected?.movie?.id ?: return
        loadMoreReviews(movieId)
    }

    private fun loadMoreReviews(movieId: Int) {
        val state = uiState.value
        if (!hasReviewPages || state.reviewsStatus in listOf(
                LoadStatus.Loading,
                LoadStatus.LoadingMore,
                LoadStatus.Error,
                LoadStatus.PaginationError
            )
        ) return
        _uiState.update {
            it.copy(
                reviewsStatus = if (it.reviews.isEmpty()) LoadStatus.Loading else LoadStatus.LoadingMore,
                reviewsError = null
            )
        }
        retryAction = ::retryReviewPage
        request = viewModelScope.launch {
            try {
                val result = getReviews(movieId, reviewPage)
                if (uiState.value.selected?.movie?.id != movieId || !uiState.value.showingReviews) return@launch
                reviewPage = result.number + 1
                hasReviewPages = result.number < result.totalPages
                _uiState.update {
                    val items = (it.reviews + result.items).distinctBy(Review::id)
                    it.copy(
                        reviews = items,
                        reviewsStatus = if (items.isEmpty()) LoadStatus.Empty else LoadStatus.Success
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        reviewsStatus = if (it.reviews.isEmpty()) LoadStatus.Error else LoadStatus.PaginationError,
                        reviewsError = message(exception, "Could not load reviews")
                    )
                }
            }
        }
    }

    private fun retryReviewPage() {
        _uiState.update { it.copy(reviewsStatus = if (it.reviews.isEmpty()) LoadStatus.Initial else LoadStatus.Success) }
        loadMoreReviews()
    }

    fun showTrailer() {
        if (uiState.value.selected?.trailer != null) _uiState.update { it.copy(showingTrailer = true) }
    }

    fun close() {
        cancelRequest()
        _uiState.update {
            when {
                it.showingTrailer -> it.copy(showingTrailer = false)
                it.showingReviews -> it.copy(showingReviews = false)
                else -> it.copy(
                    selected = null,
                    detailError = null,
                    detailStatus = LoadStatus.Initial
                )
            }
        }
    }

    fun retry() {
        if (uiState.value.genre == null) loadGenres() else retryAction?.invoke()
    }

    private fun cancelRequest() {
        request?.cancel()
        request = null
    }

    private fun message(exception: Exception, fallback: String) = when (exception) {
        is UnknownHostException, is ConnectException -> "No internet connection. Check your network and retry."
        is SocketTimeoutException -> "The request timed out. Please retry."
        is IllegalStateException, is IllegalArgumentException -> exception.message ?: fallback
        else -> fallback
    }
}
