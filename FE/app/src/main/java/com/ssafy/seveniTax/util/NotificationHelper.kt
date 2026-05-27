package com.ssafy.seveniTax.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.ssafy.seveniTax.MainActivity
import com.ssafy.seveniTax.R

object NotificationHelper {

    private const val CHANNEL_ID = "classification_channel"
    private const val CHANNEL_NAME = "세목 분류 알림"
    private const val CHANNEL_DESC = "AI 세목 자동분류 결과를 알려드립니다"

    private const val PAYMENT_CHANNEL_ID = "payment_channel"
    private const val PAYMENT_CHANNEL_NAME = "결제 알림"
    private const val PAYMENT_CHANNEL_DESC = "QR 결제 완료/실패 알림"

    private const val TAX_CALENDAR_CHANNEL_ID = "tax_calendar_channel"
    private const val TAX_CALENDAR_CHANNEL_NAME = "세금 캘린더 알림"
    private const val TAX_CALENDAR_CHANNEL_DESC = "세금 신고/납부 마감일 리마인드 알림"

    fun createNotificationChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)

        val classificationChannel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = CHANNEL_DESC
            enableVibration(true)
        }
        manager.createNotificationChannel(classificationChannel)

        val taxCalendarChannel = NotificationChannel(
            TAX_CALENDAR_CHANNEL_ID,
            TAX_CALENDAR_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = TAX_CALENDAR_CHANNEL_DESC
            enableVibration(true)
        }
        manager.createNotificationChannel(taxCalendarChannel)

        val paymentChannel = NotificationChannel(
            PAYMENT_CHANNEL_ID,
            PAYMENT_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = PAYMENT_CHANNEL_DESC
            enableVibration(true)
        }
        manager.createNotificationChannel(paymentChannel)
    }

    fun showClassificationNotification(
        context: Context,
        transactionId: String,
        merchantName: String,
        amount: String,
        aiCategory: String,
        confidence: Int
    ) {
        val manager = context.getSystemService(NotificationManager::class.java)

        // 탭 시 세목 확인 화면으로 이동하는 PendingIntent
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("navigate_to", "classification_result")
            putExtra("transaction_id", transactionId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            transactionId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 커스텀 확장 레이아웃
        val expandedView = RemoteViews(context.packageName, R.layout.notification_classification).apply {
            setTextViewText(R.id.notification_merchant, "$merchantName · $amount")
            setTextViewText(
                R.id.notification_ai_category,
                "AI 추천 세목: $aiCategory (신뢰도 ${confidence}%)"
            )
        }

        // 기본 (축소) 알림 + 확장 시 커스텀 레이아웃
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.logo_symbol)
            .setContentTitle("\uD83D\uDCB3 결제가 확인되었어요")
            .setContentText("$merchantName · $amount")
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomBigContentView(expandedView)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        manager.notify(transactionId.hashCode(), notification)
    }

    fun showTaxCalendarNotification(
        context: Context,
        taxName: String,
        deadlineDate: String,
        dDay: Int,
        additionalInfo: String? = null
    ) {
        val manager = context.getSystemService(NotificationManager::class.java)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("navigate_to", "tax_calendar")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            "tax_$taxName".hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (dDay == 0) "$taxName D-Day" else "$taxName D-$dDay"
        val body = buildString {
            if (dDay == 0) {
                append("$deadlineDate 오늘 마감입니다!")
            } else {
                append("$deadlineDate 마감 · ${dDay}일 남았습니다")
            }
            if (!additionalInfo.isNullOrEmpty()) {
                append("\n$additionalInfo")
            }
        }

        val notification = NotificationCompat.Builder(context, TAX_CALENDAR_CHANNEL_ID)
            .setSmallIcon(R.drawable.logo_symbol)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        manager.notify("tax_$taxName".hashCode(), notification)
    }

    fun showPaymentNotification(
        context: Context,
        paymentId: String,
        merchantName: String,
        amount: String,
        isSuccess: Boolean
    ) {
        val manager = context.getSystemService(NotificationManager::class.java)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            "pay_$paymentId".hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isSuccess) "결제 완료" else "결제 실패"
        val body = if (isSuccess) "$merchantName · ${amount}원 결제가 완료되었습니다"
                   else "$merchantName · ${amount}원 결제에 실패했습니다"

        val notification = NotificationCompat.Builder(context, PAYMENT_CHANNEL_ID)
            .setSmallIcon(R.drawable.logo_symbol)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        manager.notify("pay_$paymentId".hashCode(), notification)
    }
}
