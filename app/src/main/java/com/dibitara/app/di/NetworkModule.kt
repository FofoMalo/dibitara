package com.dibitara.app.di

import com.dibitara.app.BuildConfig
import com.dibitara.app.data.remote.api.FrankfurterApi
import com.dibitara.app.data.repository.ExchangeRateRepositoryImpl
import com.dibitara.app.domain.repository.ExchangeRateRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * Client HTTP avec intercepteur de logs (visible dans Logcat en mode debug uniquement).
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            // En release on ne logue pas les appels réseau (confidentialité + performance)
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                    else HttpLoggingInterceptor.Level.NONE
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
    }

    /**
     * Instance Retrofit pointant sur l'API Frankfurter (taux de change gratuits).
     * GsonConverterFactory convertit automatiquement le JSON en data class Kotlin.
     */
    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://api.frankfurter.app/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideFrankfurterApi(retrofit: Retrofit): FrankfurterApi =
        retrofit.create(FrankfurterApi::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ExchangeRateRepositoryModule {

    @Binds
    abstract fun bindExchangeRateRepository(
        impl: ExchangeRateRepositoryImpl
    ): ExchangeRateRepository
}
