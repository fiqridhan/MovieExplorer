package com.example.movieexplorer.domain

data class Genre(val id: Int, val name: String)
data class Movie(
    val id: Int,
    val title: String,
    val overview: String,
    val poster: String?,
    val rating: Double,
    val releaseDate: String? = null
)

data class Review(
    val id: String,
    val author: String,
    val content: String,
    val avatar: String? = null,
    val rating: Double? = null,
    val createdAt: String? = null
)

data class Page<T>(val items: List<T>, val number: Int, val totalPages: Int)
data class MovieDetails(
    val movie: Movie,
    val runtime: Int?,
    val releaseDate: String?,
    val status: String?,
    val trailer: String?,
    val backdrop: String? = null,
    val voteCount: Int? = null,
    val genres: List<String> = emptyList(),
    val language: String? = null
)

interface MoviesRepository {
    suspend fun genres(): List<Genre>
    suspend fun movies(genreId: Int, page: Int): Page<Movie>
    suspend fun details(movie: Movie): MovieDetails
    suspend fun reviews(movieId: Int, page: Int): Page<Review>
}

class GetGenres(private val repository: MoviesRepository) {
    suspend operator fun invoke() = repository.genres()
}

class DiscoverMovies(private val repository: MoviesRepository) {
    suspend operator fun invoke(genreId: Int, page: Int) = repository.movies(genreId, page)
}

class GetMovieDetails(private val repository: MoviesRepository) {
    suspend operator fun invoke(movie: Movie) = repository.details(movie)
}

class GetMovieReviews(private val repository: MoviesRepository) {
    suspend operator fun invoke(movieId: Int, page: Int) = repository.reviews(movieId, page)
}
