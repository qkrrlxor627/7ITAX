package com.ssafy.seveniTax.viewmodel

import androidx.lifecycle.ViewModel
import com.ssafy.seveniTax.data.local.SecureStorage
import com.ssafy.seveniTax.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val secureStorage: SecureStorage
) : ViewModel() {

    fun getAccessToken(): String =
        secureStorage.getAccessToken() ?: Constants.DEV_TOKEN
}
