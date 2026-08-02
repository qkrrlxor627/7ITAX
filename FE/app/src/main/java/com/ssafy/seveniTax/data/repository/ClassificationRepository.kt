package com.ssafy.seveniTax.data.repository

import com.ssafy.seveniTax.data.model.classification.ClassificationRequest
import com.ssafy.seveniTax.data.model.classification.ClassificationResponse
import com.ssafy.seveniTax.data.model.common.ApiResponse
import retrofit2.Response

interface ClassificationRepository {
    suspend fun classify(request: ClassificationRequest): Response<ApiResponse<ClassificationResponse>>
}
