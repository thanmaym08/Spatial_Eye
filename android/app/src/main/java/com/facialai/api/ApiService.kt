package com.facialai.api

import com.facialai.models.CheckChangesResponse
import com.facialai.models.Place
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @Multipart
    @POST("/api/save-place")
    suspend fun savePlace(
        @Part("user_id") userId: RequestBody,
        @Part("name") placeName: RequestBody,
        @Part image: MultipartBody.Part
    ): Response<Void>

    @Multipart
    @POST("/api/check-changes")
    suspend fun checkChanges(
        @Part("place_id") placeId: RequestBody,
        @Part image: MultipartBody.Part
    ): Response<CheckChangesResponse>

    @GET("/api/places")
    suspend fun getPlaces(@Query("user_id") userId: String = "default"): Response<List<Place>>

    @GET("/api/place/{place_id}")
    suspend fun getPlace(@Path("place_id") placeId: String): Response<Place>

    @DELETE("/api/place/{place_id}")
    suspend fun deletePlace(@Path("place_id") placeId: String): Response<Void>
}
