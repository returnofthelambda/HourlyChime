package com.example.hourlychime

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.time.LocalTime

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TEST_CHIME = "com.example.hourlychime.ACTION_TEST_CHIME"
        private const val TAG = "AlarmReceiver"
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

        // ⚡ Lenient window check: If the alarm fires up to 1 minute early, it's still valid.
        // Or if it fires up to 1 minute late.
        // Actually, we can just trust that if it's not a manual test, it's a scheduled alarm.
        // We only check the window to be absolutely sure we don't chime at 3 AM if something goes wrong.
        val isInWindow = currentHour in startHour..endHour || 
                         (now.minute == 59 && (currentHour + 1) in startHour..endHour)

        if (isManualTest || isInWindow) {
            Log.d(TAG, "Playing chime sound...")
            if (!isManualTest) {
                ChimeManager.setLastChimeTime(context, System.currentTimeMillis())
            }
            val pendingResult = goAsync()
            playSound(context, pendingResult)
        } else {
            Log.d(TAG, "Outside window and not a test. Skipping sound.")
        }
    }

    private fun playSound(context: Context, pendingResult: PendingResult) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        
        // 1. Log current volume levels
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        Log.d(TAG, "Current Alarm Volume: $currentVolume / $maxVolume")

        if (currentVolume == 0) {
            Log.w(TAG, "Alarm stream is muted or volume is 0!")
        }

        // 2. Build Audio Attributes
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        // 3. Request Audio Focus
        val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(audioAttributes)
            .setAcceptsDelayedFocusGain(true)
            .setOnAudioFocusChangeListener { focusChange ->
                Log.d(TAG, "Audio focus change: $focusChange")
            }
            .build()

        val result = audioManager.requestAudioFocus(focusRequest)

        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Log.w(TAG, "Audio focus request failed: $result")
        } else {
            Log.d(TAG, "Audio focus granted")
        }

        var mediaPlayer: MediaPlayer? = null
        try {
            mediaPlayer = MediaPlayer.create(
                context,
                R.raw.chime,
                audioAttributes,
                audioManager.generateAudioSessionId()
            )

            if (mediaPlayer == null) {
                Log.e(TAG, "Failed to create MediaPlayer")
                audioManager.abandonAudioFocusRequest(focusRequest)
                pendingResult.finish()
                return
            }

            // 4. Explicitly set volume to 1.0f
            mediaPlayer.setVolume(1.0f, 1.0f)
            mediaPlayer.setWakeMode(context, android.os.PowerManager.PARTIAL_WAKE_LOCK)
            
            mediaPlayer.setOnCompletionListener { mp ->
                Log.d(TAG, "Playback completed")
                mp.release()
                audioManager.abandonAudioFocusRequest(focusRequest)
                pendingResult.finish()
            }
            mediaPlayer.setOnErrorListener { mp, what, extra ->
                Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                mp.release()
                audioManager.abandonAudioFocusRequest(focusRequest)
                pendingResult.finish()
                true
            }
            
            // 5. Warm-up delay: Wear OS speakers can have latency.
            // A small delay after focus request and before start might help.
            Log.d(TAG, "Waiting 500ms for speaker warm-up...")
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    mediaPlayer.start()
                    Log.d(TAG, "MediaPlayer started")
                } catch (e: Exception) {
                    Log.e(TAG, "Error starting MediaPlayer after delay", e)
                    mediaPlayer.release()
                    audioManager.abandonAudioFocusRequest(focusRequest)
                    pendingResult.finish()
                }
            }, 500)

        } catch (e: Exception) {
            Log.e(TAG, "Exception in playSound", e)
            mediaPlayer?.release()
            audioManager.abandonAudioFocusRequest(focusRequest)
            pendingResult.finish()
        }
    }
}
