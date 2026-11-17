package com.example.movieexplorer.domain

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MoviesUseCasesTest {
    private val genre = Genre(28, "Action")
    private val movie = Movie(7, "Example", "Overview", null, 8.0)
    private val review = Review("review-1", "Reviewer", "Good movie")
    private val repository = object : MoviesRepository {
        override suspend fun genres() = listOf(genre)
        override suspend fun movies(genreId: Int, page: Int): Page<Movie> {
            require(genreId == genre.id && page > 0)
            return Page(if (page == 1) listOf(movie) else emptyList(), page, 2)
        }

        override suspend fun details(movie: Movie) =
            MovieDetails(movie, 120, "2024-01-01", "Released", null)

        override suspend fun reviews(movieId: Int, page: Int) =
            Page(if (page == 1) listOf(review) else emptyList(), page, 2)
    }

    @Test
    fun genresComeFromRepository() = runBlocking {
        assertEquals(listOf(genre), GetGenres(repository)())
    }

    @Test
    fun discoveryForwardsGenreAndPage() = runBlocking {
        assertEquals(listOf(movie), DiscoverMovies(repository)(28, 1).items)
        assertEquals(emptyList<Movie>(), DiscoverMovies(repository)(28, 2).items)
    }

    @Test
    fun detailsAndReviewsUseSelectedMovie() = runBlocking {
        assertEquals(120, GetMovieDetails(repository)(movie).runtime)
        assertEquals(listOf(review), GetMovieReviews(repository)(movie.id, 1).items)
    }

    @Test
    fun invalidPageFailsInsteadOfReturningData() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                DiscoverMovies(repository)(
                    28,
                    0
                )
            }
        }
    }
}
