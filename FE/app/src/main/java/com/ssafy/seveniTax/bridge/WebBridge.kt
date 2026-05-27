package com.ssafy.seveniTax.bridge

import android.util.Log
import android.webkit.JavascriptInterface

class WebBridge(
    private val onNavigate: (String) -> Unit,
    private val onRequestBiometric: (String) -> Unit,
    private val onRequestToken: () -> String
) {
    @JavascriptInterface
    fun navigateToNative(route: String) {
        Log.d("WebBridge", "navigateToNative: $route")
        onNavigate(route)
    }

    @JavascriptInterface
    fun requestBiometric(callbackId: String) {
        Log.d("WebBridge", "requestBiometric: $callbackId")
        onRequestBiometric(callbackId)
    }

    @JavascriptInterface
    fun getAccessToken(): String {
        return onRequestToken()
    }

    @JavascriptInterface
    fun log(message: String) {
        Log.d("WebBridge", "[Web] $message")
    }
}
