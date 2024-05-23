package com.example.socialapp.di


import androidx.annotation.Keep
import com.example.socialapp.network.UnsplashApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

@Keep
const val baseUrl = "https://api.unsplash.com/"

@Keep
val moshi: Moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()

@Keep
val retrofit: Retrofit = Retrofit.Builder()
    .addConverterFactory(MoshiConverterFactory.create(moshi))
    .baseUrl(baseUrl)
    .build()

@Keep
@Module
@InstallIn(SingletonComponent::class)
object AppModule{
    @Provides
    fun provideUnsplashApiService(): UnsplashApi = retrofit.create(UnsplashApi::class.java)
}