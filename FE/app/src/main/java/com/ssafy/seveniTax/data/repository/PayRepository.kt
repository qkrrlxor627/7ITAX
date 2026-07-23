package com.ssafy.seveniTax.data.repository

import com.ssafy.seveniTax.data.model.common.ApiResponse
import com.ssafy.seveniTax.data.model.pay.*

interface PayRepository {
    suspend fun createAccount(request: AccountCreateRequest): ApiResponse<AccountResponse>
    suspend fun getAccounts(type: String? = null): ApiResponse<List<AccountResponse>>
    suspend fun getAccount(id: String): ApiResponse<AccountResponse>
    suspend fun getBalance(accountId: String): ApiResponse<BalanceResponse>
    suspend fun setPayPin(pin: String): ApiResponse<Unit>
    suspend fun verifyPayPin(pin: String): ApiResponse<Unit>
    suspend fun getPayPinStatus(): ApiResponse<PayPinStatusResponse>
}
