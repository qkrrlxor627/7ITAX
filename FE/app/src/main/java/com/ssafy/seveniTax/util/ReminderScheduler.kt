package com.ssafy.seveniTax.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.ssafy.seveniTax.data.model.tax.TaxDeadline
import com.ssafy.seveniTax.receiver.ReminderReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun scheduleReminders(
        deadlines: List<TaxDeadline>,
        d7: Boolean,
        d3: Boolean,
        d1: Boolean,
        dDay: Boolean
    ) {
        cancelAllReminders(deadlines)

        val enabledDays = buildList {
            if (d7) add(7)
            if (d3) add(3)
            if (d1) add(1)
            if (dDay) add(0)
        }

        val now = LocalDate.now()

        for (deadline in deadlines) {
            val deadlineDate = try {
                LocalDate.parse(deadline.deadline)
            } catch (e: Exception) {
                continue
            }

            for (daysBefore in enabledDays) {
                val alarmDate = deadlineDate.minusDays(daysBefore.toLong())
                if (alarmDate.isBefore(now)) continue

                val alarmTime = alarmDate.atTime(9, 0)
                val millis = alarmTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

                val intent = Intent(context, ReminderReceiver::class.java).apply {
                    putExtra("tax_name", deadline.taxName)
                    putExtra("deadline_date", deadline.deadline)
                    putExtra("d_day", daysBefore)
                }

                val requestCode = "${deadline.taxName}_$daysBefore".hashCode()
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    millis,
                    pendingIntent
                )
            }
        }
    }

    fun cancelAllReminders(deadlines: List<TaxDeadline>) {
        val allDays = listOf(7, 3, 1, 0)
        for (deadline in deadlines) {
            for (daysBefore in allDays) {
                val requestCode = "${deadline.taxName}_$daysBefore".hashCode()
                val intent = Intent(context, ReminderReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )
                if (pendingIntent != null) {
                    alarmManager.cancel(pendingIntent)
                }
            }
        }
    }
}
