package com.mqv.vmess.activity.br

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

private const val ALARM_REPEATING_INTERVAL_MILLIS = 1 * 60 * 1000L // 1 minute

class AlarmSleepTimer(private val context: Context) {
    fun setRepeatingAlarm() {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val alarmIntent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent =
            PendingIntent.getBroadcast(context, 0, alarmIntent, 0)

        if (pendingIntent != null) {
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis(),
                ALARM_REPEATING_INTERVAL_MILLIS,
                pendingIntent
            )
        }
    }
}