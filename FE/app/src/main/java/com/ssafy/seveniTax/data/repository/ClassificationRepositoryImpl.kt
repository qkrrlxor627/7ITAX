package com.ssafy.seveniTax.data.repository

import com.ssafy.seveniTax.data.model.classification.ClassificationRequest
import com.ssafy.seveniTax.data.model.classification.ClassificationResponse
import com.ssafy.seveniTax.data.model.common.ApiResponse
import com.ssafy.seveniTax.data.remote.ClassificationApi
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClassificationRepositoryImpl @Inject constructor(
    private val api: ClassificationApi
) : ClassificationRepository {

    override suspend fun classify(request: ClassificationRequest): Response<ApiResponse<ClassificationResponse>> =
        api.classify(request)
}
