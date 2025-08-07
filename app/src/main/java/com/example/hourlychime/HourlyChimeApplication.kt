package com.example.hourlychime

import android.app.Application

class HourlyChimeApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // When the app starts, check if the chime was enabled.
        // If so, schedule it. This handles cases like device reboots.
        val isEnabled = ChimeManager.isChimeEnabled(this)
        if (isEnabled) {
            ChimeManager.scheduleOrCancelChime(this, true)
        }
    }
}
