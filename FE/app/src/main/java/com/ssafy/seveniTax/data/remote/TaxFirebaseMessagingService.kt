package com.ssafy.seveniTax.data.remote

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.ssafy.seveniTax.util.NotificationHelper

class TaxFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM Token: $token")
        // TODO: 서버에 토큰 전송
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "FCM Message: ${message.data}")

        val data = message.data
        val type = data["type"]

        when (type) {
            "classification" -> handleClassificationNotification(data)
            "tax_calendar" -> handleTaxCalendarNotification(data)
            "payment" -> handlePaymentNotification(data)
            else -> handleDefaultNotification(message)
        }
    }

    private fun handleClassificationNotification(data: Map<String, String>) {
        val transactionId = data["transactionId"] ?: return
        val merchantName = data["merchantName"] ?: "알 수 없음"
        val amount = data["amount"] ?: "0원"
        val aiCategory = data["aiCategory"] ?: "미분류"
        val confidence = data["confidence"]?.toIntOrNull() ?: 0

        NotificationHelper.showClassificationNotification(
            context = this,
            transactionId = transactionId,
            merchantName = merchantName,
            amount = amount,
            aiCategory = aiCategory,
            confidence = confidence
        )
    }

    private fun handleTaxCalendarNotification(data: Map<String, String>) {
        val taxName = data["taxName"] ?: return
        val deadlineDate = data["deadlineDate"] ?: ""
        val dDay = data["dDay"]?.toIntOrNull() ?: 0
        val additionalInfo = data["additionalInfo"]

        NotificationHelper.showTaxCalendarNotification(
            context = this,
            taxName = taxName,
            deadlineDate = deadlineDate,
            dDay = dDay,
            additionalInfo = additionalInfo
        )
    }

    private fun handlePaymentNotification(data: Map<String, String>) {
        val paymentId = data["paymentId"] ?: return
        val merchantName = data["merchantName"] ?: "가맹점"
        val amount = data["amount"] ?: "0"
        val status = data["status"] ?: "CAPTURED"

        NotificationHelper.showPaymentNotification(
            context = this,
            paymentId = paymentId,
            merchantName = merchantName,
            amount = amount,
            isSuccess = status == "CAPTURED"
        )
    }

    private fun handleDefaultNotification(message: RemoteMessage) {
        message.notification?.let { notification ->
            NotificationHelper.showClassificationNotification(
                context = this,
                transactionId = System.currentTimeMillis().toString(),
                merchantName = notification.body ?: "",
                amount = "",
                aiCategory = "",
                confidence = 0
            )
        }
    }

    companion object {
        private const val TAG = "TaxFCM"
    }
}
