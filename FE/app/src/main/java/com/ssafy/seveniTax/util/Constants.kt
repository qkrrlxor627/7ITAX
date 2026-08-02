package com.ssafy.seveniTax.util

import com.ssafy.seveniTax.BuildConfig

object Constants {
    val API_BASE_URL = if (BuildConfig.DEBUG)
        "http://10.0.2.2:8080/api/"
    else
        "https://j14c203.p.ssafy.io/api/"

    val WEBVIEW_BASE_URL = if (BuildConfig.DEBUG)
        "http://10.0.2.2:3000"
    else
        "https://j14c203.p.ssafy.io"

    // 개발 중 토큰 하드코딩 분기
    const val DEV_TOKEN = "dev_access_token_placeholder"

    // SecureStorage keys
    const val KEY_ACCESS_TOKEN = "access_token"
    const val KEY_REFRESH_TOKEN = "refresh_token"
    const val KEY_USER_ID = "user_id"
    const val KEY_PHONE_NUMBER = "phone_number"
    const val KEY_USER_NAME = "user_name"
    const val KEY_PAY_ENROLLED = "pay_enrolled"

    // PIN
    const val PIN_LENGTH = 6
}
