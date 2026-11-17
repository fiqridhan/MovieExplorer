package com.example.movieexplorer

import com.example.movieexplorer.data.TmdbMoviesRepository
import com.example.movieexplorer.domain.MoviesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MoviesModule {
    @Provides
    @Singleton
    fun provideRepository(): MoviesRepository = TmdbMoviesRepository(BuildConfig.TMDB_TOKEN)
}
