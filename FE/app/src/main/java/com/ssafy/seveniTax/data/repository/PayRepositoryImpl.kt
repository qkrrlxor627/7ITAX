package com.ssafy.seveniTax.data.repository

import com.ssafy.seveniTax.data.model.common.ApiResponse
import com.ssafy.seveniTax.data.model.pay.*
import com.ssafy.seveniTax.data.remote.PayApi
import org.json.JSONObject
import retrofit2.Response
import javax.inject.Inject

class PayRepositoryImpl @Inject constructor(
    private val payApi: PayApi
) : PayRepository {

    override suspend fun createAccount(request: AccountCreateRequest): ApiResponse<AccountResponse> {
        val response = payApi.createAccount(request)
        return response.body() ?: throw Exception(extractErrorMessage(response, "계좌 생성에 실패했습니다."))
    }

    override suspend fun getAccounts(type: String?): ApiResponse<List<AccountResponse>> {
        val response = payApi.getAccounts(type)
        return response.body() ?: throw Exception(extractErrorMessage(response, "계좌 목록 조회에 실패했습니다."))
    }

    override suspend fun getAccount(id: String): ApiResponse<AccountResponse> {
        val response = payApi.getAccount(id)
        return response.body() ?: throw Exception(extractErrorMessage(response, "계좌 조회에 실패했습니다."))
    }

    override suspend fun getBalance(accountId: String): ApiResponse<BalanceResponse> {
        val response = payApi.getBalance(accountId)
        return response.body() ?: throw Exception(extractErrorMessage(response, "잔액 조회에 실패했습니다."))
    }

    override suspend fun setPayPin(pin: String): ApiResponse<Unit> {
        val response = payApi.setPayPin(PayPinRequest(pin))
        return response.body() ?: throw Exception(extractErrorMessage(response, "결제 비밀번호 설정에 실패했습니다."))
    }

    override suspend fun verifyPayPin(pin: String): ApiResponse<Unit> {
        val response = payApi.verifyPayPin(PayPinRequest(pin))
        return response.body() ?: throw Exception(extractErrorMessage(response, "결제 비밀번호 확인에 실패했습니다."))
    }

    override suspend fun getPayPinStatus(): ApiResponse<PayPinStatusResponse> {
        val response = payApi.getPayPinStatus()
        return response.body() ?: throw Exception(extractErrorMessage(response, "결제 비밀번호 상태 조회에 실패했습니다."))
    }

    private fun <T> extractErrorMessage(response: Response<T>, fallback: String): String {
        return try {
            val errorBody = response.errorBody()?.string()
            if (errorBody != null) {
                JSONObject(errorBody).optString("message", fallback)
            } else fallback
        } catch (_: Exception) { fallback }
    }
}
