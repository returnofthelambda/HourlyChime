package com.example.hourlychime

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.hourlychime.presentation.MainActivity
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

object ChimeManager {

    private const val PREFS_NAME = "chime_prefs"
    private const val KEY_CHIME_ENABLED = "chime_enabled"
    private const val KEY_START_HOUR = "start_hour"
    private const val KEY_END_HOUR = "end_hour"
    private const val KEY_LAST_CHIME_TIME = "last_chime_time"
    private const val KEY_NEXT_CHIME_TIME = "next_chime_time"
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

    fun setLastChimeTime(context: Context, timeMillis: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_LAST_CHIME_TIME, timeMillis).apply()
    }

    fun getLastChimeTime(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_LAST_CHIME_TIME, 0L)
    }

    fun setNextChimeTime(context: Context, timeMillis: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_NEXT_CHIME_TIME, timeMillis).apply()
    }

    fun getNextChimeTime(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_NEXT_CHIME_TIME, 0L)
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
        // ⚡ Bolt Optimization: Replace Calendar with java.time for better performance
        // and reduced object allocation.
        val now = ZonedDateTime.now()
        val startHour = getStartHour(context)
        val endHour = getEndHour(context)

        val nextChimeTime = calculateNextChimeTime(now, startHour, endHour)

        val triggerAtMillis = nextChimeTime.toInstant().toEpochMilli()
        setNextChimeTime(context, triggerAtMillis)
        setAlarm(context, triggerAtMillis)
    }

    @androidx.annotation.VisibleForTesting
    internal fun calculateNextChimeTime(now: ZonedDateTime, startHour: Int, endHour: Int): ZonedDateTime {
        val currentHour = now.hour
        // Start with the top of the current hour
        var nextChime = now.truncatedTo(ChronoUnit.HOURS)

        if (currentHour < startHour) {
            // Case 1: Before the window today. Next chime is at the start hour today.
            nextChime = nextChime.withHour(startHour)
        } else if (currentHour >= endHour) {
            // Case 2: After the window today (or at the very last chime hour).
            // Next chime is at the start hour tomorrow.
            nextChime = nextChime.plusDays(1).withHour(startHour)
        } else {
            // Case 3: Within the window. Next chime is at the top of the next hour.
            nextChime = nextChime.plusHours(1)
        }

        // ⚡ Bolt Optimization: Ensure the next chime is at least 1 second in the future.
        // This prevents immediate re-triggering if the alarm fires slightly early.
        if (!nextChime.isAfter(now)) {
            nextChime = nextChime.plusHours(1)
            // Re-check window if we had to push it forward
            if (nextChime.hour > endHour) {
                nextChime = nextChime.plusDays(1).withHour(startHour)
            }
        }

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

        // Using setAlarmClock for maximum reliability. 
        // Note: This shows an alarm icon on the watch face. 
        // If a "quieter" background chime is preferred, setExactAndAllowWhileIdle could be used,
        // but it is more likely to be throttled by the system.
        alarmManager.setAlarmClock(alarmClockInfo, alarmPendingIntent)
    }

    fun cancelChime(context: Context) {
        setNextChimeTime(context, 0L)
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
