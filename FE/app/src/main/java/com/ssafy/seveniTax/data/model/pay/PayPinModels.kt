package com.ssafy.seveniTax.data.model.pay

data class PayPinRequest(
    val pin: String
)

data class PayPinStatusResponse(
    val registered: Boolean
)
