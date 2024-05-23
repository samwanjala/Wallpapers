package com.example.socialapp.network

import androidx.annotation.Keep
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.Query

const val clientId = "Rc-B_dkNSwkdzQ4MBAtu_tx8zdOwdSRgSKCGDeN9c70"

@Keep
interface UnsplashApi{
    @Headers("Authorization: Client-ID $clientId")
    @GET("topics")
    suspend fun getTopics(
        @Query("page") page: Int = 0,
        @Query("per_page") pageSize: Int = 20,
        @Query("order_by") orderBy: String = "featured"
    ): Response<List<ListTopic>>

    @Headers("Authorization: Client-ID $clientId")
    @GET("topics/{slug}/photos")
    suspend fun getTopicPhotos(
        @Path("slug") topicSlug: String,
        @Query("page") page: Int = 0,
        @Query("per_page") pageSize: Int = 20
    ): Response<List<ListPhoto>>

    @Headers("Authorization: Client-ID $clientId")
    @GET("photos")
    suspend fun getPhotos(
        @Query("page") page: Int = 0,
        @Query("per_page") pageSize: Int = 20,
        @Query("order_by") orderBy: String = "latest"
    ): Response<List<ListPhoto>>
}