package com.example.hourlychime

import android.content.Context
import android.media.MediaPlayer
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.hourlychime.R

class HourlyChimeWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    override fun doWork(): Result {
        // Create and play the chime sound
        val mediaPlayer = MediaPlayer.create(applicationContext, R.raw.chime)
        mediaPlayer.setOnCompletionListener {
            // Release the media player once the sound has finished playing
            it.release()
        }
        mediaPlayer.start()

        // Indicate that the work finished successfully
        return Result.success()
    }
}

