package com.example.hourlychime.presentation

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Switch
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.ToggleChip
import com.example.hourlychime.ChimeManager
import com.example.hourlychime.R

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WearApp()
        }
    }
}

@Composable
fun WearApp() {
    val context = LocalContext.current
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    // --- NEW PERMISSION HANDLING LOGIC ---

    // A launcher to request the exact alarm permission.
    // When the user returns from the settings screen, this will re-check the permission.
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        // After the user returns, we don't need to do anything here,
        // because the check will happen again when they tap the toggle.
    }

    // Function to check if the app has the required permission.
    fun hasPermission(): Boolean {
        // For modern Android versions, we need to check canScheduleExactAlarms.
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            // On older versions, the manifest permission is enough.
            true
        }
    }

    // --- UI STATE ---

    // The initial state of the toggle is now based on BOTH the saved preference AND if the permission is granted.
    var isChecked by remember {
        mutableStateOf(ChimeManager.isChimeEnabled(context) && hasPermission())
    }

    MaterialTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.title1,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                ToggleChip(
                    modifier = Modifier.fillMaxWidth(),
                    checked = isChecked,
                    onCheckedChange = { newCheckedState ->
                        if (newCheckedState) {
                            // When turning the chime ON
                            if (hasPermission()) {
                                // If we have permission, schedule the chime and update the UI.
                                ChimeManager.setChimeEnabled(context, true)
                                ChimeManager.scheduleOrCancelChime(context, true)
                                isChecked = true
                            } else {
                                // If we DON'T have permission, launch the system settings screen.
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                    permissionLauncher.launch(intent)
                                }
                            }
                        } else {
                            // When turning the chime OFF, just cancel it.
                            ChimeManager.setChimeEnabled(context, false)
                            ChimeManager.scheduleOrCancelChime(context, false)
                            isChecked = false
                        }
                    },
                    label = {
                        Text(text = stringResource(R.string.hourly_chime_label))
                    },
                    toggleControl = {
                        Switch(checked = isChecked)
                    }
                )
            }
        }
    }
}

