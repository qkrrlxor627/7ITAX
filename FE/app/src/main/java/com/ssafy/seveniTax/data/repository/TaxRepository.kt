package com.ssafy.seveniTax.data.repository

import com.ssafy.seveniTax.data.model.classification.*
import com.ssafy.seveniTax.data.model.common.ApiResponse
import com.ssafy.seveniTax.data.model.tax.*
import okhttp3.ResponseBody
import retrofit2.Response

interface TaxRepository {
    suspend fun classify(body: ClassificationRequest): Response<ApiResponse<ClassificationResponse>>
    suspend fun getDeadlines(): Response<ApiResponse<List<TaxDeadline>>>
    suspend fun getEstimation(year: Int?): Response<ApiResponse<TaxEstimationResponse>>
    suspend fun exportBookEntries(year: Int?): Response<ResponseBody>
    suspend fun exportVat(year: Int?, half: Int?): Response<ResponseBody>
    suspend fun exportIncomeTax(year: Int?): Response<ResponseBody>
    suspend fun exportLocalTax(year: Int?): Response<ResponseBody>
    suspend fun exportVatExcel(year: Int?, half: Int?): Response<ResponseBody>
    suspend fun exportIncomeTaxExcel(year: Int?): Response<ResponseBody>
    suspend fun exportSimpleLedgerPdf(year: Int?): Response<ResponseBody>
}
