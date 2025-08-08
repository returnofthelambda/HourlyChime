package com.example.hourlychime

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // This method is called when the alarm fires.

        // --- NEW AUDIO HANDLING LOGIC ---
        try {
            val mediaPlayer = MediaPlayer()

            // Set the audio attributes to use the ALARM stream.
            // This ensures the sound respects the user's alarm volume setting.
            mediaPlayer.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )

            // Set the sound file as the data source.
            val soundUri = Uri.parse(
                "android.resource://${context.packageName}/${R.raw.chime}"
            )
            mediaPlayer.setDataSource(context, soundUri)

            // Prepare and start the player.
            mediaPlayer.prepare()
            mediaPlayer.start()

            // Release the player once the sound is finished.
            mediaPlayer.setOnCompletionListener {
                it.release()
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
        // --- END OF NEW LOGIC ---


        // IMPORTANT: Reschedule the next alarm to keep the cycle going.
        ChimeManager.scheduleNextChime(context)
    }
}
