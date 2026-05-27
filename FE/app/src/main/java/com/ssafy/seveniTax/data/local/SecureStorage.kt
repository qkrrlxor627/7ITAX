package com.ssafy.seveniTax.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.ssafy.seveniTax.util.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs by lazy {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

        EncryptedSharedPreferences.create(
            "7itax_secure_prefs",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun getAccessToken(): String? = prefs.getString(Constants.KEY_ACCESS_TOKEN, null)

    fun saveAccessToken(token: String) {
        prefs.edit().putString(Constants.KEY_ACCESS_TOKEN, token).apply()
    }

    fun getRefreshToken(): String? = prefs.getString(Constants.KEY_REFRESH_TOKEN, null)

    fun saveRefreshToken(token: String) {
        prefs.edit().putString(Constants.KEY_REFRESH_TOKEN, token).apply()
    }

    fun saveUserId(userId: String) {
        prefs.edit().putString(Constants.KEY_USER_ID, userId).apply()
    }

    fun getUserId(): String? = prefs.getString(Constants.KEY_USER_ID, null)

    fun savePhoneNumber(phone: String) {
        prefs.edit().putString(Constants.KEY_PHONE_NUMBER, phone).apply()
    }

    fun getPhoneNumber(): String? = prefs.getString(Constants.KEY_PHONE_NUMBER, null)

    fun saveUserName(name: String) {
        prefs.edit().putString(Constants.KEY_USER_NAME, name).apply()
    }

    fun getUserName(): String? = prefs.getString(Constants.KEY_USER_NAME, null)

    fun setPayEnrolled(enrolled: Boolean) {
        prefs.edit().putBoolean(Constants.KEY_PAY_ENROLLED, enrolled).apply()
    }

    fun isPayEnrolled(): Boolean = prefs.getBoolean(Constants.KEY_PAY_ENROLLED, false)

    fun isLoggedIn(): Boolean = getAccessToken() != null

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
