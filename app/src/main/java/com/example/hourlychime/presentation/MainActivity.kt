package com.example.hourlychime.presentation

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.core.content.ContextCompat
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.*
import androidx.wear.compose.material.dialog.Dialog
import com.example.hourlychime.ChimeManager
import com.example.hourlychime.PomodoroManager
import com.example.hourlychime.R
import java.text.SimpleDateFormat
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.Locale
import kotlinx.coroutines.delay

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
	var lastChimeTime by remember { mutableStateOf(ChimeManager.getLastChimeTime(context)) }
	var nextChimeTime by remember { mutableStateOf(ChimeManager.getNextChimeTime(context)) }
	
	// --- State for Pomodoro ---
	var pomodoroState by remember { mutableStateOf(PomodoroManager.getState(context)) }
	var pomodoroEndTime by remember { mutableStateOf(PomodoroManager.getEndTime(context)) }
	var currentTimeMillis by remember { mutableStateOf(System.currentTimeMillis()) }
	
	var showTimePickerDialog by remember { mutableStateOf(false) }
	var isEditingStartHour by remember { mutableStateOf(true) }
	var isEditingPomodoroDuration by remember { mutableStateOf(false) }
	var isEditingWorkDuration by remember { mutableStateOf(true) }

	// Countdown Timer
	LaunchedEffect(pomodoroState, pomodoroEndTime) {
		if (pomodoroState != PomodoroManager.State.IDLE) {
			while (true) {
				currentTimeMillis = System.currentTimeMillis()
				if (currentTimeMillis >= pomodoroEndTime && pomodoroEndTime > 0) {
					pomodoroState = PomodoroManager.State.IDLE
					pomodoroEndTime = 0L
					break
				}
				delay(1000)
			}
		}
	}

	fun hasPermission(): Boolean {
		val hasExactAlarm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
			alarmManager.canScheduleExactAlarms()
		} else {
			true
		}
		
		val hasNotifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
		} else {
			true
		}
		
		return hasExactAlarm && hasNotifications
	}

	// --- Permission Handling Logic ---
	val exactAlarmLauncher = rememberLauncherForActivityResult(
			contract = ActivityResultContracts.StartActivityForResult()
			) {
		if (hasPermission()) {
			isEnabled = true
				ChimeManager.setChimeEnabled(context, true)
				ChimeManager.scheduleOrCancelChime(context, true)
				nextChimeTime = ChimeManager.getNextChimeTime(context)
		}
	}

	val notificationPermissionLauncher = rememberLauncherForActivityResult(
		contract = ActivityResultContracts.RequestPermission()
	) { isGranted ->
		if (isGranted) {
			if (hasPermission()) {
				isEnabled = true
				ChimeManager.setChimeEnabled(context, true)
				ChimeManager.scheduleOrCancelChime(context, true)
				nextChimeTime = ChimeManager.getNextChimeTime(context)
			} else {
				// Still need exact alarm?
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
					val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
					exactAlarmLauncher.launch(intent)
				}
			}
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
						verticalArrangement = Arrangement.spacedBy(4.dp)
						) {
					item {
						Text(
								text = stringResource(R.string.app_name),
								style = MaterialTheme.typography.title2, // Slightly smaller
								color = MaterialTheme.colors.primary,
								modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
							)
					}

					// --- Pomodoro Section ---
					item {
						Text(
							text = "Focus Timer",
							style = MaterialTheme.typography.caption1,
							color = MaterialTheme.colors.secondary
						)
					}

					item {
						val remainingMillis = (pomodoroEndTime - currentTimeMillis).coerceAtLeast(0L)
						val minutes = (remainingMillis / 1000) / 60
						val seconds = (remainingMillis / 1000) % 60
						
						if (pomodoroState == PomodoroManager.State.IDLE) {
							Row(
								modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
								horizontalArrangement = Arrangement.spacedBy(4.dp)
							) {
								CompactChip(
									onClick = {
										PomodoroManager.startSession(context, PomodoroManager.State.WORK)
										pomodoroState = PomodoroManager.State.WORK
										pomodoroEndTime = PomodoroManager.getEndTime(context)
									},
									label = { Text("Focus") },
									modifier = Modifier.weight(1f)
								)
								CompactChip(
									onClick = {
										PomodoroManager.startSession(context, PomodoroManager.State.BREAK)
										pomodoroState = PomodoroManager.State.BREAK
										pomodoroEndTime = PomodoroManager.getEndTime(context)
									},
									label = { Text("Break") },
									modifier = Modifier.weight(1f),
									colors = ChipDefaults.secondaryChipColors()
								)
							}
						} else {
							Card(
								onClick = { /* Could show details */ },
								modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp)
							) {
								Column(horizontalAlignment = Alignment.CenterHorizontally) {
									Text(
										text = String.format(Locale.US, "%02d:%02d", minutes, seconds),
										style = MaterialTheme.typography.display3,
										fontWeight = FontWeight.Bold
									)
									Text(
										text = if (pomodoroState == PomodoroManager.State.WORK) "Focusing" else "On Break",
										style = MaterialTheme.typography.caption2
									)
									Spacer(modifier = Modifier.height(4.dp))
									Button(
										onClick = {
											PomodoroManager.stopSession(context)
											pomodoroState = PomodoroManager.State.IDLE
											pomodoroEndTime = 0L
										},
										modifier = Modifier.size(ButtonDefaults.SmallButtonSize),
										colors = ButtonDefaults.secondaryButtonColors()
									) {
										Text("Stop")
									}
								}
							}
						}
					}

					item {
						Chip(
							modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
							label = { Text("Focus Duration", style = MaterialTheme.typography.caption2) },
							secondaryLabel = { Text("${PomodoroManager.getWorkDuration(context)}m") },
							onClick = {
								isEditingPomodoroDuration = true
								isEditingWorkDuration = true
								showTimePickerDialog = true
							}
						)
					}
					item {
						Chip(
							modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
							label = { Text("Break Duration", style = MaterialTheme.typography.caption2) },
							secondaryLabel = { Text("${PomodoroManager.getBreakDuration(context)}m") },
							onClick = {
								isEditingPomodoroDuration = true
								isEditingWorkDuration = false
								showTimePickerDialog = true
							}
						)
					}

					// --- Hourly Chime Section ---
					item {
						Spacer(modifier = Modifier.height(8.dp))
						Text(
							text = "Hourly Chime",
							style = MaterialTheme.typography.caption1,
							color = MaterialTheme.colors.secondary
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
								nextChimeTime = ChimeManager.getNextChimeTime(context)
								} else {
								// Request Notifications first on Android 13+
								if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && 
									ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
									notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
								} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
								val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
								exactAlarmLauncher.launch(intent)
								}
								}
								} else {
								isEnabled = false
								ChimeManager.setChimeEnabled(context, false)
								ChimeManager.scheduleOrCancelChime(context, false)
								nextChimeTime = 0L
								}
								},
								label = { Text("Enable Chime") },
								toggleControl = { Switch(checked = isEnabled) }
						)
					}

					item {
						Chip(
								modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
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

					if (isEnabled && nextChimeTime > 0) {
						item {
							Text(
								text = "Next: ${formatDateTime(nextChimeTime)}",
								style = MaterialTheme.typography.caption2,
								color = MaterialTheme.colors.secondary,
								modifier = Modifier.padding(top = 4.dp)
							)
						}
					}

					if (lastChimeTime > 0) {
						item {
							Text(
								text = "Last: ${formatDateTime(lastChimeTime)}",
								style = MaterialTheme.typography.caption2,
								modifier = Modifier.padding(top = 2.dp)
							)
						}
					}

					item {
						Button(
								onClick = {
								// Instantly trigger the AlarmReceiver logic on demand
								val testIntent = Intent(context, com.example.hourlychime.AlarmReceiver::class.java).apply {
									action = com.example.hourlychime.AlarmReceiver.ACTION_TEST_CHIME
								}
								context.sendBroadcast(testIntent)
								},
								modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
								colors = ButtonDefaults.secondaryButtonColors()
							  ) {
							Text("Test Chime")
						}
					}
				}
		}
	}

	// --- Time Picker Dialog ---
	TimePickerDialog(
			showDialog = showTimePickerDialog,
			initialValue = if (isEditingPomodoroDuration) {
				if (isEditingWorkDuration) PomodoroManager.getWorkDuration(context) else PomodoroManager.getBreakDuration(context)
			} else {
				if (isEditingStartHour) startHour else endHour
			},
			isMinutes = isEditingPomodoroDuration,
			onDismiss = { showTimePickerDialog = false },
			onSelected = { selectedValue ->
				if (isEditingPomodoroDuration) {
					if (isEditingWorkDuration) {
						PomodoroManager.setWorkDuration(context, selectedValue)
					} else {
						PomodoroManager.setBreakDuration(context, selectedValue)
					}
				} else {
					if (isEditingStartHour) {
						startHour = selectedValue
						ChimeManager.setStartHour(context, selectedValue)
					} else {
						endHour = selectedValue
						ChimeManager.setEndHour(context, selectedValue)
					}
					// If the chime is already enabled, reschedule it with the new times
					if (isEnabled) {
						ChimeManager.scheduleNextChime(context)
						nextChimeTime = ChimeManager.getNextChimeTime(context)
					}
				}
				showTimePickerDialog = false
				isEditingPomodoroDuration = false
			}
			)
}

private fun formatDateTime(millis: Long): String {
	val date = Date(millis)
	val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
	return formatter.format(date)
}

@Composable
fun TimePickerDialog(
		showDialog: Boolean,
		initialValue: Int,
		isMinutes: Boolean = false,
		onDismiss: () -> Unit,
		onSelected: (Int) -> Unit
		) {
	val numberOfOptions = if (isMinutes) 61 else 24
	val pickerState = rememberPickerState(
			initialNumberOfOptions = numberOfOptions,
			initiallySelectedOption = initialValue
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
				item { Text(if (isMinutes) "Select Minutes" else "Select Hour", style = MaterialTheme.typography.title2) }

				item {
					Picker(
							state = pickerState,
							modifier = Modifier.height(100.dp),
							separation = 4.dp,
							contentDescription = "Select value"
						  ) { index ->
						Text(
								text = if (isMinutes) "$index m" else formatHour(index),
								style = MaterialTheme.typography.title2
							)
					}
				}

				item {
					Button(
							onClick = { onSelected(pickerState.selectedOption) },
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
private val formattedHoursCache = arrayOfNulls<String>(24)

	private fun getHourFormatter(): DateTimeFormatter {
		val currentLocale = Locale.getDefault()
			if (cachedLocale != currentLocale || cachedFormatter == null) {
				cachedLocale = currentLocale
					cachedFormatter = DateTimeFormatter.ofPattern("h a", currentLocale)
					// Clear cache when locale changes
					formattedHoursCache.fill(null)
			}
		return cachedFormatter!!
	}

// Helper function to format hour for display (e.g., 8 -> 8:00 AM)
private fun formatHour(hour: Int): String {
	// ⚡ Bolt Optimization: Cache formatted hour strings for the finite set of 24 hours.
	// This achieves zero object allocation when scrolling the time picker.
	val cached = formattedHoursCache[hour]
		if (cached != null) return cached

			val formatted = LocalTime.of(hour, 0).format(getHourFormatter())
				formattedHoursCache[hour] = formatted
				return formatted
}
