package com.ssafy.seveniTax.data.remote

import com.ssafy.seveniTax.data.model.common.ApiResponse
import com.ssafy.seveniTax.data.model.tax.TaxEstimationResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface TaxEstimationApi {

    @GET("tax-estimation")
    suspend fun getEstimation(
        @Query("year") year: Int? = null
    ): Response<ApiResponse<TaxEstimationResponse>>
}
