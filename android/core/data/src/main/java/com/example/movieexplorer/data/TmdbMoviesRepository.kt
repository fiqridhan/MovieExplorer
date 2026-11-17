package com.example.movieexplorer.data

import com.example.movieexplorer.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlin.time.Duration.Companion.milliseconds

class TmdbMoviesRepository(
    private val token: String,
    private val client: OkHttpClient = OkHttpClient.Builder().callTimeout(15, TimeUnit.SECONDS)
        .build(),
    private val baseUrl: String = "https://api.themoviedb.org/3/"
) : MoviesRepository {
    private suspend fun get(path: String) = withContext(Dispatchers.IO) {
        require(token.isNotBlank()) { "Set TMDB_ACCESS_TOKEN in local.properties." }
        val request = Request.Builder().url("$baseUrl$path")
            .header("Authorization", "Bearer $token").build()
        repeat(2) { attempt ->
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if ((response.code == 429 || response.code >= 500) && attempt == 0) {
                    delay(300.milliseconds)
                    return@use
                }
                check(response.isSuccessful) {
                    val message = when (response.code) {
                        401 -> "Invalid TMDB token"
                        403 -> "TMDB access denied"
                        404 -> "Movie unavailable"
                        429 -> "Too many requests; retry shortly"
                        in 500..599 -> "TMDB server unavailable"
                        else -> runCatching { JSONObject(body).optString("status_message") }.getOrDefault(
                            "Request failed"
                        )
                    }
                    "TMDB ${response.code}: $message"
                }
                return@withContext JSONObject(body)
            }
        }
        error("TMDB request failed")
    }

    private fun JSONObject.asMovie() = Movie(
        optInt("id"),
        optString("title"),
        optString("overview"),
        optString("poster_path").takeIf { it.isNotBlank() && it != "null" }
            ?.let { "https://image.tmdb.org/t/p/w500$it" },
        optDouble("vote_average"),
        optString("release_date").takeIf { it.isNotBlank() }
    )

    override suspend fun genres(): List<Genre> =
        get("genre/movie/list?language=en-US").getJSONArray("genres").let { a ->
            (0 until a.length()).map {
                a.getJSONObject(it).let { it1 -> Genre(it1.getInt("id"), it1.getString("name")) }
            }
        }

    override suspend fun movies(genreId: Int, page: Int): Page<Movie> =
        get("discover/movie?with_genres=$genreId&sort_by=popularity.desc&include_adult=false&page=$page").let { result ->
            val array = result.getJSONArray("results")
            Page(
                (0 until array.length()).map { array.getJSONObject(it).asMovie() },
                result.optInt("page", page),
                result.optInt("total_pages", page)
            )
        }

    override suspend fun details(movie: Movie): MovieDetails {
        val info = get("movie/${movie.id}?language=en-US")
        val videos = try {
            get("movie/${movie.id}/videos?language=en-US").getJSONArray("results")
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            null // Details remain usable when the optional videos endpoint fails.
        }
        val trailer = videos?.let { array ->
            (0 until array.length()).map { array.getJSONObject(it) }
                .filter {
                    it.optString("site") == "YouTube" && it.optString("key")
                        .matches(Regex("[A-Za-z0-9_-]{11}"))
                }
                .sortedWith(compareByDescending<JSONObject> { it.optString("type") == "Trailer" }
                    .thenByDescending { it.optBoolean("official") }
                    .thenBy { it.optString("key") })
                .firstOrNull()?.optString("key")?.let { "https://www.youtube.com/watch?v=$it" }
        }
        return MovieDetails(
            info.asMovie(),
            info.optInt("runtime").takeIf { it > 0 },
            info.optString("release_date").takeIf { it.isNotBlank() },
            info.optString("status").takeIf { it.isNotBlank() },
            trailer,
            info.optString("backdrop_path").takeIf { it.isNotBlank() && it != "null" }
                ?.let { "https://image.tmdb.org/t/p/w780$it" },
            info.optInt("vote_count"),
            info.optJSONArray("genres")?.let { genres ->
                (0 until genres.length()).map {
                    genres.getJSONObject(it).optString("name")
                }
            } ?: emptyList(),
            info.optString("original_language").takeIf { it.isNotBlank() }
        )
    }

    override suspend fun reviews(movieId: Int, page: Int): Page<Review> =
        get("movie/$movieId/reviews?page=$page").let { result ->
            val array = result.getJSONArray("results")
            Page((0 until array.length()).map {
                array.getJSONObject(it).let { it1 ->
                    Review(
                        it1.optString("id"),
                        it1.optString("author"),
                        it1.optString("content"),
                        it1.optJSONObject("author_details")?.optString("avatar_path")?.let { path ->
                            when {
                                path.startsWith("/https://") -> path.removePrefix("/")
                                path.startsWith("/") -> "https://image.tmdb.org/t/p/w185$path"
                                else -> null
                            }
                        },
                        it1.optJSONObject("author_details")?.optDouble("rating")
                            ?.takeIf { rating -> !rating.isNaN() && rating > 0 },
                        it1.optString("created_at").takeIf { date -> date.isNotBlank() }
                    )
                }
            }, result.optInt("page", page), result.optInt("total_pages", page))
        }
}
