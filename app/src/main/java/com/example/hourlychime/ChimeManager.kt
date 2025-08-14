package com.example.hourlychime

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.hourlychime.presentation.MainActivity
import java.util.Calendar

object ChimeManager {

    private const val PREFS_NAME = "chime_prefs"
    private const val KEY_CHIME_ENABLED = "chime_enabled"
    private const val KEY_START_HOUR = "start_hour"
    private const val KEY_END_HOUR = "end_hour"
    private const val ALARM_REQUEST_CODE = 1001

    // --- Preference Management ---

    fun setChimeEnabled(context: Context, isEnabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_CHIME_ENABLED, isEnabled).apply()
    }

    fun isChimeEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_CHIME_ENABLED, false)
    }

    fun setStartHour(context: Context, hour: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_START_HOUR, hour).apply()
    }

    fun getStartHour(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_START_HOUR, 8) // Default start time: 8 AM
    }

    fun setEndHour(context: Context, hour: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_END_HOUR, hour).apply()
    }

    fun getEndHour(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_END_HOUR, 22) // Default end time: 10 PM
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
        val now = Calendar.getInstance()
        val startHour = getStartHour(context)
        val endHour = getEndHour(context)

        val nextChimeTime = calculateNextChimeTime(now, startHour, endHour)

        // If nextChimeTime is null, it means we are outside the active window and should not schedule.
        // However, the logic in calculateNextChimeTime should always return a valid future time.
        setAlarm(context, nextChimeTime.timeInMillis)
    }

    private fun calculateNextChimeTime(now: Calendar, startHour: Int, endHour: Int): Calendar {
        val currentHour = now.get(Calendar.HOUR_OF_DAY)
        val nextChime = Calendar.getInstance()

        if (currentHour < startHour) {
            // Case 1: Before the window today. Next chime is at the start hour today.
            nextChime.set(Calendar.HOUR_OF_DAY, startHour)
        } else if (currentHour >= endHour) {
            // Case 2: After the window today. Next chime is at the start hour tomorrow.
            nextChime.add(Calendar.DAY_OF_YEAR, 1)
            nextChime.set(Calendar.HOUR_OF_DAY, startHour)
        } else {
            // Case 3: Within the window. Next chime is at the top of the next hour.
            nextChime.add(Calendar.HOUR_OF_DAY, 1)
        }

        // Set minutes and seconds to 0 for a precise top-of-the-hour chime.
        nextChime.set(Calendar.MINUTE, 0)
        nextChime.set(Calendar.SECOND, 0)
        nextChime.set(Calendar.MILLISECOND, 0)

        return nextChime
    }


    private fun setAlarm(context: Context, triggerAtMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmReceiverIntent = Intent(context, AlarmReceiver::class.java)
        val alarmPendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            alarmReceiverIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showAppIntent = Intent(context, MainActivity::class.java)
        val showAppPendingIntent = PendingIntent.getActivity(context, 0, showAppIntent, PendingIntent.FLAG_IMMUTABLE)
        val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, showAppPendingIntent)

        alarmManager.setAlarmClock(alarmClockInfo, alarmPendingIntent)
    }

    fun cancelChime(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}
