package com.ssafy.seveniTax.data.model.common

data class ApiResponse<T>(
    val status: String,
    val message: String = "",
    val errorCode: String? = null,
    val data: T? = null
)

data class PageResponse<T>(
    val content: List<T> = emptyList(),
    val totalElements: Long = 0,
    val totalPages: Int = 0,
    val size: Int = 0,
    val number: Int = 0,
    val first: Boolean = true,
    val last: Boolean = true
)
