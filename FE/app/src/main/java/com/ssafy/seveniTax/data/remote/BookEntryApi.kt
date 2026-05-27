package com.ssafy.seveniTax.data.remote

import com.ssafy.seveniTax.data.model.book.*
import com.ssafy.seveniTax.data.model.common.ApiResponse
import com.ssafy.seveniTax.data.model.common.PageResponse
import retrofit2.Response
import retrofit2.http.*

interface BookEntryApi {

    @POST("book-entries")
    suspend fun createEntry(@Body body: BookEntryRequest): Response<ApiResponse<BookEntryResponse>>

    @GET("book-entries")
    suspend fun getEntries(
        @Query("confirmed") confirmed: Boolean? = null,
        @Query("entryType") entryType: String? = null,
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null,
        @Query("categoryCode") categoryCode: String? = null,
        @Query("keyword") keyword: String? = null,
        @Query("hasEvidence") hasEvidence: Boolean? = null,
        @Query("unclassified") unclassified: Boolean? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("sort") sort: String? = null
    ): Response<ApiResponse<PageResponse<BookEntryResponse>>>

    @GET("book-entries/{entryId}")
    suspend fun getEntry(@Path("entryId") entryId: Long): Response<ApiResponse<BookEntryResponse>>

    @GET("book-entries/unconfirmed-count")
    suspend fun getUnconfirmedCount(): Response<ApiResponse<Int>>

    @PATCH("book-entries/{entryId}/confirm")
    suspend fun confirmEntry(@Path("entryId") entryId: Long): Response<ApiResponse<BookEntryResponse>>

    @PATCH("book-entries/{entryId}/category")
    suspend fun updateCategory(
        @Path("entryId") entryId: Long,
        @Body body: CategoryUpdateRequest
    ): Response<ApiResponse<BookEntryResponse>>

    @PATCH("book-entries/{entryId}/note")
    suspend fun updateNote(
        @Path("entryId") entryId: Long,
        @Body body: NoteUpdateRequest
    ): Response<ApiResponse<BookEntryResponse>>

    @PATCH("book-entries/{entryId}/personal")
    suspend fun markAsPersonal(@Path("entryId") entryId: Long): Response<ApiResponse<BookEntryResponse>>

    @PATCH("book-entries/{entryId}/business")
    suspend fun markAsBusiness(@Path("entryId") entryId: Long): Response<ApiResponse<BookEntryResponse>>
}
