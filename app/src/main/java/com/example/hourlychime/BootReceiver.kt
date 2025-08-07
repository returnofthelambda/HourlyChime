package com.example.hourlychime

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Check if the user had the chime enabled before the reboot.
            if (ChimeManager.isChimeEnabled(context)) {
                // If so, reschedule the alarm.
                ChimeManager.scheduleNextChime(context)
            }
        }
    }
}
