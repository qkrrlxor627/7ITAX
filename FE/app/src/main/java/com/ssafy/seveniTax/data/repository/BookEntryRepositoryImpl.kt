package com.ssafy.seveniTax.data.repository

import com.ssafy.seveniTax.data.model.book.*
import com.ssafy.seveniTax.data.model.common.ApiResponse
import com.ssafy.seveniTax.data.model.common.PageResponse
import com.ssafy.seveniTax.data.remote.BookEntryApi
import retrofit2.Response
import javax.inject.Inject

class BookEntryRepositoryImpl @Inject constructor(
    private val api: BookEntryApi
) : BookEntryRepository {
    override suspend fun createEntry(body: BookEntryRequest) = api.createEntry(body)
    override suspend fun getEntries(
        confirmed: Boolean?,
        entryType: String?,
        startDate: String?,
        endDate: String?,
        categoryCode: String?,
        keyword: String?,
        hasEvidence: Boolean?,
        unclassified: Boolean?,
        page: Int,
        size: Int
    ) = api.getEntries(confirmed, entryType, startDate, endDate, categoryCode, keyword, hasEvidence, unclassified, page, size)
    override suspend fun getEntry(entryId: Long) = api.getEntry(entryId)
    override suspend fun getUnconfirmedCount() = api.getUnconfirmedCount()
    override suspend fun confirmEntry(entryId: Long) = api.confirmEntry(entryId)
    override suspend fun updateCategory(entryId: Long, body: CategoryUpdateRequest) = api.updateCategory(entryId, body)
    override suspend fun markAsPersonal(entryId: Long) = api.markAsPersonal(entryId)
    override suspend fun markAsBusiness(entryId: Long) = api.markAsBusiness(entryId)
    override suspend fun updateNote(entryId: Long, body: NoteUpdateRequest) = api.updateNote(entryId, body)
}
