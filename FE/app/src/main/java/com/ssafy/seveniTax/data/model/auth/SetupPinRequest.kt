package com.ssafy.seveniTax.data.model.auth

data class ConsentRequest(
    val consentType: String,
    val agreed: Boolean
)

data class SetupPinRequest(
    val pin: String,
    val deviceId: String,
    val deviceName: String,
    val consents: List<ConsentRequest>
)
