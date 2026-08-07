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
        private const val TAG = "AlarmReceiver"
        private const val CHANNEL_ID = "hourly_chime_channel"
        private const val NOTIFICATION_ID = 1002
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive: action=${intent.action}")
        
        val isManualTest = intent.action == ACTION_TEST_CHIME
        val isChimeEnabled = ChimeManager.isChimeEnabled(context)

        // Always try to schedule the next chime if enabled.
        if (isChimeEnabled) {
            ChimeManager.scheduleNextChime(context)
        } else if (!isManualTest) {
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

        if (isManualTest || isInWindow) {
            Log.d(TAG, "Triggering chime notification...")
            if (!isManualTest) {
                ChimeManager.setLastChimeTime(context, System.currentTimeMillis())
            }
            showChimeNotification(context)
        } else {
            Log.d(TAG, "Outside window and not a test. Skipping.")
        }
    }

    private fun showChimeNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // 1. Create Channel (Android 8.0+)
        val soundUri = Uri.parse("android.resource://${context.packageName}/${R.raw.chime}")
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        // ⚡ Stealth Mode: Use IMPORTANCE_DEFAULT to try to avoid the "heads-up" pop-up,
        // but keep the sound associated with the channel.
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Hourly Chime",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Hourly chime alerts"
            setSound(soundUri, audioAttributes)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 100)
        }
        notificationManager.createNotificationChannel(channel)

        // 2. Build Notification (Minimal text for stealth)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Hourly Chime") // Updated title
            .setPriority(NotificationCompat.PRIORITY_DEFAULT) // Don't pop up
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 100))
            .setAutoCancel(true)
            .setTimeoutAfter(1000) // Auto-remove after 1 second
            .build()

        // 3. Fire!
        notificationManager.notify(NOTIFICATION_ID, notification)
        Log.d(TAG, "Stealth notification posted")
    }
}
