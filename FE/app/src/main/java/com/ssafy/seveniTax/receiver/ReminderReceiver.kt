package com.ssafy.seveniTax.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ssafy.seveniTax.util.NotificationHelper

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taxName = intent.getStringExtra("tax_name") ?: return
        val deadlineDate = intent.getStringExtra("deadline_date") ?: return
        val dDay = intent.getIntExtra("d_day", -1)
        if (dDay < 0) return

        NotificationHelper.showTaxCalendarNotification(
            context = context,
            taxName = taxName,
            deadlineDate = deadlineDate,
            dDay = dDay
        )
    }
}
