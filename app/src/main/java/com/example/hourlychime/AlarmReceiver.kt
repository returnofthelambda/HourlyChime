package com.example.hourlychime

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // This method is called when the alarm fires.

        // Play the chime sound.
        val mediaPlayer = MediaPlayer.create(context, R.raw.chime)
        mediaPlayer?.setOnCompletionListener {
            it.release()
        }
        mediaPlayer?.start()

        // IMPORTANT: Reschedule the next alarm because repeating alarms can be inexact.
        // By rescheduling every time, we ensure it stays accurate.
        ChimeManager.scheduleNextChime(context)
    }
}
