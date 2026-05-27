package com.ssafy.seveniTax

import android.app.Application
import com.ssafy.seveniTax.util.NotificationHelper
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SevenITaxApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannel(this)
    }
}
