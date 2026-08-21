package com.example.hourlychime

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.util.Log
import androidx.core.app.NotificationCompat
import java.time.LocalTime

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TEST_CHIME = "com.example.hourlychime.ACTION_TEST_CHIME"
        const val ACTION_HOURLY_CHIME = "com.example.hourlychime.ACTION_HOURLY_CHIME"
        const val ACTION_POMODORO_DONE = "com.example.hourlychime.ACTION_POMODORO_DONE"
        
        private const val TAG = "AlarmReceiver"
        private const val CHANNEL_ID = "tempo_alerts_channel_v2"
        private const val NOTIFICATION_ID = 2001
        private const val POMODORO_NOTIFICATION_ID = 2002
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val hasPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        
        Log.d(TAG, "onReceive: action=$action, hasNotificationPermission=$hasPermission")
        
        when (action) {
            ACTION_TEST_CHIME -> {
                showChimeNotification(context)
            }
            ACTION_POMODORO_DONE -> {
                showPomodoroNotification(context)
            }
            else -> {
                handleHourlyChime(context)
            }
        }
    }

    private fun handleHourlyChime(context: Context) {
        val isChimeEnabled = ChimeManager.isChimeEnabled(context)

        if (isChimeEnabled) {
            ChimeManager.scheduleNextChime(context)
        } else {
            Log.d(TAG, "Chime disabled, skipping.")
            return
        }

        val startHour = ChimeManager.getStartHour(context)
        val endHour = ChimeManager.getEndHour(context)
        val now = LocalTime.now()
        val currentHour = now.hour

        Log.d(TAG, "Current time: $now, Window: $startHour-$endHour")

        val isInWindow = currentHour in startHour..endHour || 
                         (now.minute == 59 && (currentHour + 1) == startHour)

        if (isInWindow) {
            Log.d(TAG, "Triggering hourly chime...")
            ChimeManager.setLastChimeTime(context, System.currentTimeMillis())
            showChimeNotification(context)
        } else {
            Log.d(TAG, "Outside window. Skipping.")
        }
    }

    private fun showChimeNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val soundUri = Uri.parse("android.resource://${context.packageName}/${R.raw.chime}")
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Tempo Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            setSound(soundUri, audioAttributes)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 100)
        }
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Tempo Chime")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 100))
            .setAutoCancel(true)
            .setTimeoutAfter(2000)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
        Log.d(TAG, "Chime notification sent")
    }

    private fun showPomodoroNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val soundUri = Uri.parse("android.resource://${context.packageName}/${R.raw.chime}")
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        // Reuse the same channel but ensure high importance
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Tempo Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            setSound(soundUri, audioAttributes)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 200, 100, 200)
        }
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Focus Timer")
            .setContentText("Session finished")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 200, 100, 200))
            .setAutoCancel(true)
            .build()

        notificationManager.notify(POMODORO_NOTIFICATION_ID, notification)
        Log.d(TAG, "Pomodoro notification sent")
    }
}
