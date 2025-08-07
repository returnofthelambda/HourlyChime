package com.example.hourlychime

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

object ChimeManager {

    private const val PREFS_NAME = "chime_prefs"
    private const val KEY_CHIME_ENABLED = "chime_enabled"
    private const val ALARM_REQUEST_CODE = 1001

    // --- Preference Management (No changes here) ---

    fun setChimeEnabled(context: Context, isEnabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_CHIME_ENABLED, isEnabled).apply()
    }

    fun isChimeEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_CHIME_ENABLED, false)
    }

    // --- Alarm Scheduling Logic ---

    fun scheduleOrCancelChime(context: Context, isEnabled: Boolean) {
        if (isEnabled) {
            scheduleNextChime(context)
        } else {
            cancelChime(context)
        }
    }

    fun scheduleNextChime(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Calculate the time for the next hour
        val nextHour = Calendar.getInstance().apply {
            add(Calendar.HOUR_OF_DAY, 1)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Use setExactAndAllowWhileIdle to ensure the alarm fires even in doze mode.
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            nextHour.timeInMillis,
            pendingIntent
        )
    }

    private fun cancelChime(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        // If the alarm exists, cancel it.
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}

