package com.example.hourlychime

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences

object PomodoroManager {
    private const val PREFS_NAME = "pomodoro_prefs"
    private const val KEY_STATE = "pomodoro_state"
    private const val KEY_END_TIME = "pomodoro_end_time"
    private const val KEY_WORK_DURATION = "pomodoro_work_duration"
    private const val KEY_BREAK_DURATION = "pomodoro_break_duration"

    private const val ALARM_REQUEST_CODE = 1002

    enum class State {
        IDLE, WORK, BREAK
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getState(context: Context): State {
        val name = getPrefs(context).getString(KEY_STATE, State.IDLE.name)
        return try { State.valueOf(name!!) } catch (e: Exception) { State.IDLE }
    }

    fun setState(context: Context, state: State) {
        getPrefs(context).edit().putString(KEY_STATE, state.name).apply()
    }

    fun getEndTime(context: Context): Long {
        return getPrefs(context).getLong(KEY_END_TIME, 0L)
    }

    fun setEndTime(context: Context, time: Long) {
        getPrefs(context).edit().putLong(KEY_END_TIME, time).apply()
    }

    fun getWorkDuration(context: Context): Int {
        return getPrefs(context).getInt(KEY_WORK_DURATION, 25) // Default 25m
    }

    fun setWorkDuration(context: Context, minutes: Int) {
        getPrefs(context).edit().putInt(KEY_WORK_DURATION, minutes).apply()
    }

    fun getBreakDuration(context: Context): Int {
        return getPrefs(context).getInt(KEY_BREAK_DURATION, 5) // Default 5m
    }

    fun setBreakDuration(context: Context, minutes: Int) {
        getPrefs(context).edit().putInt(KEY_BREAK_DURATION, minutes).apply()
    }

    fun startSession(context: Context, state: State) {
        val durationMinutes = if (state == State.WORK) getWorkDuration(context) else getBreakDuration(context)
        val endTime = System.currentTimeMillis() + (durationMinutes * 60 * 1000L)
        
        setState(context, state)
        setEndTime(context, endTime)
        
        scheduleAlarm(context, endTime)
        updateTile(context)
    }

    fun stopSession(context: Context) {
        setState(context, State.IDLE)
        setEndTime(context, 0L)
        cancelAlarm(context)
        updateTile(context)
    }

    private fun updateTile(context: Context) {
        try {
            androidx.wear.tiles.TileService.getUpdater(context)
                .requestUpdate(PomodoroTileService::class.java)
        } catch (e: Exception) {
            android.util.Log.e("PomodoroManager", "Error updating tile", e)
        }
    }

    private fun scheduleAlarm(context: Context, triggerAtMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        // Check for exact alarm permission on Android 12+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                // Fallback to inexact if permission is missing, or let the UI handle it.
                // For now, we'll try to set it and let the UI's permission check handle the gating.
                android.util.Log.w("PomodoroManager", "Missing exact alarm permission!")
            }
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_POMODORO_DONE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showAppIntent = Intent(context, com.example.hourlychime.presentation.MainActivity::class.java)
        val showAppPendingIntent = PendingIntent.getActivity(context, 0, showAppIntent, PendingIntent.FLAG_IMMUTABLE)
        val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, showAppPendingIntent)
        
        try {
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
        } catch (e: SecurityException) {
            android.util.Log.e("PomodoroManager", "SecurityException scheduling alarm", e)
        }
    }

    private fun cancelAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_POMODORO_DONE
        }
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
