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
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.*
import androidx.wear.compose.material.dialog.Dialog
import com.example.hourlychime.ChimeManager
import com.example.hourlychime.R
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.Locale

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
    // ⚡ Bolt Optimization: Removed `val hours = (0..23).toList()` to prevent
    // an ArrayList and Integer objects allocation on every recomposition.
    // The Picker index exactly maps to the hour value (0-23).
    val pickerState = rememberPickerState(
        initialNumberOfOptions = 24,
        initiallySelectedOption = initialHour
    )
    val listState = rememberScalingLazyListState()

    Dialog(
        showDialog = showDialog,
        onDismissRequest = onDismiss,
    ) {
        ScalingLazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
        ) {
            item { Text("Select Hour", style = MaterialTheme.typography.title2) }

            item {
                Picker(
                    state = pickerState,
                    modifier = Modifier.height(100.dp),
                    separation = 4.dp,
                    contentDescription = "Select hour"
                ) { hourIndex ->
                    Text(
                        text = formatHour(hourIndex),
                        style = MaterialTheme.typography.title2
                    )
                }
            }

            item {
                Button(
                    onClick = { onTimeSelected(pickerState.selectedOption) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Confirm") }
            }

            item {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.secondaryButtonColors(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Cancel") }
            }
        }
    }
}

// Cached formatter and pre-computed strings to prevent recreation overhead
// in UI lists (especially Compose Pickers), while responding dynamically
// to user Locale changes.
private var cachedLocale: Locale? = null
private var cachedFormatter: DateTimeFormatter? = null
private val cachedHourStrings = Array<String?>(24) { null }

private fun getHourFormatter(currentLocale: Locale): DateTimeFormatter {
    if (cachedLocale != currentLocale || cachedFormatter == null) {
        cachedLocale = currentLocale
        cachedFormatter = DateTimeFormatter.ofPattern("h a", currentLocale)
        // Clear string cache when locale changes
        for (i in cachedHourStrings.indices) {
            cachedHourStrings[i] = null
        }
    }
    return cachedFormatter!!
}

// Helper function to format hour for display (e.g., 8 -> 8:00 AM)
private fun formatHour(hour: Int): String {
    // ⚡ Bolt Optimization: Fully memoize the formatted hour strings.
    // The previous optimization still allocated LocalTime and String objects
    // on every render loop inside the Compose Picker. Caching the final strings
    // eliminates these allocations entirely.
    val currentLocale = Locale.getDefault()
    val formatter = getHourFormatter(currentLocale)

    if (cachedHourStrings[hour] == null) {
        cachedHourStrings[hour] = LocalTime.of(hour, 0).format(formatter)
    }
    return cachedHourStrings[hour]!!
}
