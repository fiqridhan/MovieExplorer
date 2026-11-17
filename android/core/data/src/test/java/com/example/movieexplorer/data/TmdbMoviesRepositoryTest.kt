package com.example.movieexplorer.data

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TmdbMoviesRepositoryTest {
    private val server = MockWebServer().apply { start() }
    private val repository =
        TmdbMoviesRepository("test-token", baseUrl = server.url("/3/").toString())

    @After
    fun close() = server.shutdown()

    @Test
    fun genresUseOfficialEndpointAndBearerToken() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"genres":[{"id":28,"name":"Action"}]}"""))
        assertEquals("Action", repository.genres().single().name)
        val request = server.takeRequest()
        assertEquals("/3/genre/movie/list?language=en-US", request.path)
        assertEquals("Bearer test-token", request.getHeader("Authorization"))
    }

    @Test
    fun discoverPreservesLastPageMetadata() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"page":2,"total_pages":2,"results":[{"id":7,"title":"Film","overview":"Text","vote_average":7.5}]}"""))
        val result = repository.movies(28, 2)
        assertEquals(2, result.totalPages)
        assertEquals(7, result.items.single().id)
        assertEquals("28", server.takeRequest().requestUrl?.queryParameter("with_genres"))
    }

    @Test
    fun unauthorizedResponseFails() {
        server.enqueue(
            MockResponse().setResponseCode(401).setBody("""{"status_message":"Invalid token"}""")
        )
        val error =
            assertThrows(IllegalStateException::class.java) { runBlocking { repository.genres() } }
        assert(error.message?.contains("401") == true)
    }

    @Test
    fun trailerPrefersOfficialYouTubeAndIgnoresInvalidKeys() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"id":7,"title":"Film","overview":"Text","vote_average":8.0,"genres":[]}"""))
        server.enqueue(MockResponse().setBody("""{"results":[{"site":"YouTube","type":"Trailer","official":false,"key":"AAAAAAAAAAA"},{"site":"YouTube","type":"Trailer","official":true,"key":"BBBBBBBBBBB"},{"site":"YouTube","type":"Trailer","official":true,"key":"invalid key"}]}"""))
        val detail =
            repository.details(com.example.movieexplorer.domain.Movie(7, "Film", "", null, 8.0))
        assertEquals("https://www.youtube.com/watch?v=BBBBBBBBBBB", detail.trailer)
    }

    @Test
    fun detailsRemainAvailableWhenVideosFail() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"id":7,"title":"Film","overview":"Text","vote_average":8.0,"genres":[]}"""))
        server.enqueue(MockResponse().setResponseCode(404))
        val detail =
            repository.details(com.example.movieexplorer.domain.Movie(7, "Film", "", null, 8.0))
        assertEquals("Film", detail.movie.title)
        assertEquals(null, detail.trailer)
    }

    @Test
    fun rateLimitRetriesOnceThenSucceeds() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(429))
        server.enqueue(MockResponse().setBody("""{"genres":[{"id":28,"name":"Action"}]}"""))
        assertEquals("Action", repository.genres().single().name)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun emptyReviewPageRemainsEmpty() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"page":1,"total_pages":1,"results":[]}"""))
        val page = repository.reviews(7, 1)
        assertEquals(0, page.items.size)
        assertEquals(1, page.totalPages)
    }

    @Test
    fun accessDeniedDoesNotRetry() {
        server.enqueue(MockResponse().setResponseCode(403))
        val error =
            assertThrows(IllegalStateException::class.java) { runBlocking { repository.genres() } }
        assert(error.message?.contains("access denied") == true)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun serverFailureRetriesOnlyOnce() {
        server.enqueue(MockResponse().setResponseCode(503))
        server.enqueue(MockResponse().setResponseCode(503))
        val error =
            assertThrows(IllegalStateException::class.java) { runBlocking { repository.genres() } }
        assert(error.message?.contains("server unavailable") == true)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun missingMovieReturnsResourceErrorWithoutRetry() {
        server.enqueue(MockResponse().setResponseCode(404))
        val error = assertThrows(IllegalStateException::class.java) {
            runBlocking {
                repository.details(
                    com.example.movieexplorer.domain.Movie(
                        7,
                        "Film",
                        "",
                        null,
                        8.0
                    )
                )
            }
        }
        assert(error.message?.contains("Movie unavailable") == true)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun malformedResponseFailsInsteadOfReturningFakeGenres() {
        server.enqueue(MockResponse().setBody("not-json"))
        assertThrows(org.json.JSONException::class.java) { runBlocking { repository.genres() } }
        assertEquals(1, server.requestCount)
    }
}
