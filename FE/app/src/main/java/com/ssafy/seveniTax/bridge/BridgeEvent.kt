package com.ssafy.seveniTax.bridge

data class BridgeEvent(
    val type: BridgeEventType,
    val payload: Map<String, String> = emptyMap()
)

enum class BridgeEventType {
    TOKEN_READY,
    PAY_COMPLETE,
    BIOMETRIC_RESULT,
    NAVIGATE,
    LOGOUT
}
