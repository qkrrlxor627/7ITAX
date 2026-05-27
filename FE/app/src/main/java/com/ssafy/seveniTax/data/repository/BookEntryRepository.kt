package com.ssafy.seveniTax.data.repository

import com.ssafy.seveniTax.data.model.book.*
import com.ssafy.seveniTax.data.model.common.ApiResponse
import com.ssafy.seveniTax.data.model.common.PageResponse
import retrofit2.Response

interface BookEntryRepository {
    suspend fun createEntry(body: BookEntryRequest): Response<ApiResponse<BookEntryResponse>>
    suspend fun getEntries(
        confirmed: Boolean? = null,
        entryType: String? = null,
        startDate: String? = null,
        endDate: String? = null,
        categoryCode: String? = null,
        keyword: String? = null,
        hasEvidence: Boolean? = null,
        unclassified: Boolean? = null,
        page: Int = 0,
        size: Int = 20
    ): Response<ApiResponse<PageResponse<BookEntryResponse>>>
    suspend fun getEntry(entryId: Long): Response<ApiResponse<BookEntryResponse>>
    suspend fun getUnconfirmedCount(): Response<ApiResponse<Int>>
    suspend fun confirmEntry(entryId: Long): Response<ApiResponse<BookEntryResponse>>
    suspend fun updateCategory(entryId: Long, body: CategoryUpdateRequest): Response<ApiResponse<BookEntryResponse>>
    suspend fun markAsPersonal(entryId: Long): Response<ApiResponse<BookEntryResponse>>
    suspend fun markAsBusiness(entryId: Long): Response<ApiResponse<BookEntryResponse>>
    suspend fun updateNote(entryId: Long, body: NoteUpdateRequest): Response<ApiResponse<BookEntryResponse>>
}
