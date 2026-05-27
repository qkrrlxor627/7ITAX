package com.ssafy.seveniTax.data.repository

import com.ssafy.seveniTax.data.model.classification.*
import com.ssafy.seveniTax.data.model.common.ApiResponse
import com.ssafy.seveniTax.data.model.tax.*
import com.ssafy.seveniTax.data.remote.ClassificationApi
import com.ssafy.seveniTax.data.remote.ExportApi
import com.ssafy.seveniTax.data.remote.TaxCalendarApi
import com.ssafy.seveniTax.data.remote.TaxEstimationApi
import okhttp3.ResponseBody
import retrofit2.Response
import javax.inject.Inject

class TaxRepositoryImpl @Inject constructor(
    private val classificationApi: ClassificationApi,
    private val taxCalendarApi: TaxCalendarApi,
    private val taxEstimationApi: TaxEstimationApi,
    private val exportApi: ExportApi
) : TaxRepository {
    override suspend fun classify(body: ClassificationRequest) = classificationApi.classify(body)
    override suspend fun getDeadlines() = taxCalendarApi.getDeadlines()
    override suspend fun getEstimation(year: Int?) = taxEstimationApi.getEstimation(year)
    override suspend fun exportBookEntries(year: Int?) = exportApi.exportBookEntries(year)
    override suspend fun exportVat(year: Int?, half: Int?) = exportApi.exportVat(year, half)
    override suspend fun exportIncomeTax(year: Int?) = exportApi.exportIncomeTax(year)
    override suspend fun exportLocalTax(year: Int?) = exportApi.exportLocalTax(year)
    override suspend fun exportVatExcel(year: Int?, half: Int?) = exportApi.exportVatExcel(year, half)
    override suspend fun exportIncomeTaxExcel(year: Int?) = exportApi.exportIncomeTaxExcel(year)
    override suspend fun exportSimpleLedgerPdf(year: Int?) = exportApi.exportSimpleLedgerPdf(year)
}
