package com.ssafy.seveniTax.data.remote

import com.ssafy.seveniTax.data.model.classification.*
import com.ssafy.seveniTax.data.model.common.ApiResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ClassificationApi {

    @POST("classification")
    suspend fun classify(@Body body: ClassificationRequest): Response<ApiResponse<ClassificationResponse>>
}
