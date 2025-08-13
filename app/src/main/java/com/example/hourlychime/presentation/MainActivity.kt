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
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
import androidx.wear.compose.material.dialog.Alert
import androidx.wear.compose.material.dialog.Dialog
import androidx.wear.compose.material.rememberPickerState
import com.example.hourlychime.ChimeManager
import com.example.hourlychime.R
import java.text.SimpleDateFormat
import java.util.*

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

    // --- State for UI ---
    var isEnabled by remember { mutableStateOf(ChimeManager.isChimeEnabled(context)) }
    var startHour by remember { mutableStateOf(ChimeManager.getStartHour(context)) }
    var endHour by remember { mutableStateOf(ChimeManager.getEndHour(context)) }
    var showTimePickerDialog by remember { mutableStateOf(false) }
    var isEditingStartHour by remember { mutableStateOf(true) }

    // --- Permission Handling Logic ---
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {}

    fun hasPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    // --- Main UI ---
    MaterialTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            timeText = { TimeText() }
        ) {
            val listState = rememberScalingLazyListState()
            ScalingLazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                item {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.title1,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                item {
                    ToggleChip(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                        checked = isEnabled,
                        onCheckedChange = { newCheckedState ->
                            if (newCheckedState) {
                                if (hasPermission()) {
                                    isEnabled = true
                                    ChimeManager.setChimeEnabled(context, true)
                                    ChimeManager.scheduleOrCancelChime(context, true)
                                } else {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                        permissionLauncher.launch(intent)
                                    }
                                }
                            } else {
                                isEnabled = false
                                ChimeManager.setChimeEnabled(context, false)
                                ChimeManager.scheduleOrCancelChime(context, false)
                            }
                        },
                        label = { Text("Enable Chime") },
                        toggleControl = { Switch(checked = isEnabled) }
                    )
                }

                item {
                    Chip(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
                        label = { Text("Start Time") },
                        secondaryLabel = { Text(formatHour(startHour)) },
                        onClick = {
                            isEditingStartHour = true
                            showTimePickerDialog = true
                        }
                    )
                }

                item {
                    Chip(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                        label = { Text("End Time") },
                        secondaryLabel = { Text(formatHour(endHour)) },
                        onClick = {
                            isEditingStartHour = false
                            showTimePickerDialog = true
                        }
                    )
                }
            }
        }
    }

    // --- Time Picker Dialog ---
    TimePickerDialog(
        showDialog = showTimePickerDialog,
        initialHour = if (isEditingStartHour) startHour else endHour,
        onDismiss = { showTimePickerDialog = false },
        onTimeSelected = { selectedHour ->
            if (isEditingStartHour) {
                startHour = selectedHour
                ChimeManager.setStartHour(context, selectedHour)
            } else {
                endHour = selectedHour
                ChimeManager.setEndHour(context, selectedHour)
            }
            // If the chime is already enabled, reschedule it with the new times
            if (isEnabled) {
                ChimeManager.scheduleNextChime(context)
            }
            showTimePickerDialog = false
        }
    )
}

@Composable
fun TimePickerDialog(
    showDialog: Boolean,
    initialHour: Int,
    onDismiss: () -> Unit,
    onTimeSelected: (Int) -> Unit
) {
    val hours = (0..23).toList()
    val pickerState = rememberPickerState(
        initialNumberOfOptions = hours.size,
        initiallySelectedOption = hours.indexOf(initialHour)
    )

    // The Dialog composable handles showing and hiding the dialog.
    Dialog(
        showDialog = showDialog,
        onDismissRequest = onDismiss,
    ) {
        // The Alert composable provides the standard dialog layout.
        Alert(
            title = { Text("Select Hour", textAlign = TextAlign.Center) },
            positiveButton = {
                Button(
                    onClick = { onTimeSelected(hours[pickerState.selectedOption]) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Confirm") }
            },
            negativeButton = {
                 Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.secondaryButtonColors(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Cancel") }
            }
        ) {
            // The content of the Alert is a ColumnScope, so we can place our Picker here.
            Picker(
                state = pickerState,
                modifier = Modifier.height(100.dp),
                separation = 4.dp
            ) { hourIndex ->
                Text(
                    text = formatHour(hours[hourIndex]),
                    style = MaterialTheme.typography.title2
                )
            }
        }
    }
}

// Helper function to format hour for display (e.g., 8 -> 8:00 AM)
private fun formatHour(hour: Int): String {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, 0)
    }
    // Using a simple 12-hour format with AM/PM
    return SimpleDateFormat("h a", Locale.getDefault()).format(calendar.time)
}

