package com.example.hourlychime

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import java.util.Calendar

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val startHour = ChimeManager.getStartHour(context)
        val endHour = ChimeManager.getEndHour(context)
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        // Only play the sound if the current hour is within the active window.
        // This is a safeguard; the alarm shouldn't fire outside this window anyway.
        if (currentHour in startHour..endHour) {
            playSound(context)
        }

        // Always try to schedule the next chime. The ChimeManager will figure out
        // if the next one is in an hour or tomorrow morning.
        if (ChimeManager.isChimeEnabled(context)) {
            ChimeManager.scheduleNextChime(context)
        }
    }

    private fun playSound(context: Context) {
        try {
            val mediaPlayer = MediaPlayer()
            mediaPlayer.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            val soundUri = Uri.parse("android.resource://${context.packageName}/${R.raw.chime}")
            mediaPlayer.setDataSource(context, soundUri)
            mediaPlayer.prepare()
            mediaPlayer.start()
            mediaPlayer.setOnCompletionListener { it.release() }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
