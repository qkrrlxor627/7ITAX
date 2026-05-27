package com.ssafy.seveniTax.data.remote

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ExportApi {

    @GET("export/book-entries")
    suspend fun exportBookEntries(
        @Query("year") year: Int? = null
    ): Response<ResponseBody>

    @GET("export/vat")
    suspend fun exportVat(
        @Query("year") year: Int? = null,
        @Query("half") half: Int? = null
    ): Response<ResponseBody>

    @GET("export/income-tax")
    suspend fun exportIncomeTax(
        @Query("year") year: Int? = null
    ): Response<ResponseBody>

    @GET("export/local-tax")
    suspend fun exportLocalTax(
        @Query("year") year: Int? = null
    ): Response<ResponseBody>

    @GET("export/vat/excel")
    suspend fun exportVatExcel(
        @Query("year") year: Int? = null,
        @Query("half") half: Int? = null
    ): Response<ResponseBody>

    @GET("export/income-tax/excel")
    suspend fun exportIncomeTaxExcel(
        @Query("year") year: Int? = null
    ): Response<ResponseBody>

    @GET("export/simple-ledger/pdf")
    suspend fun exportSimpleLedgerPdf(
        @Query("year") year: Int? = null
    ): Response<ResponseBody>
}
