package com.ssafy.seveniTax.data.remote

import com.ssafy.seveniTax.data.model.common.ApiResponse
import com.ssafy.seveniTax.data.model.tax.TaxDeadline
import retrofit2.Response
import retrofit2.http.GET

interface TaxCalendarApi {

    @GET("tax-calendar/deadlines")
    suspend fun getDeadlines(): Response<ApiResponse<List<TaxDeadline>>>
}
