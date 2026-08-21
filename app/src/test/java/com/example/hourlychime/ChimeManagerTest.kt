package com.example.hourlychime

import android.app.AlarmManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.ZonedDateTime

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class ChimeManagerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun calculateNextChimeTime_beforeWindow_setsToStartHourToday() {
        val now = ZonedDateTime.now().withHour(6).withMinute(30).withSecond(0).withNano(0)
        val startHour = 8
        val endHour = 22
        val nextChime = ChimeManager.calculateNextChimeTime(now, startHour, endHour)

        assertEquals("Hour should be the start hour", startHour, nextChime.hour)
        assertEquals("Minute should be 0", 0, nextChime.minute)
        assertEquals("Day should be the same", now.dayOfYear, nextChime.dayOfYear)
    }

    @Test
    fun calculateNextChimeTime_afterWindow_setsToStartHourTomorrow() {
        val now = ZonedDateTime.now().withHour(22).withMinute(15).withSecond(0).withNano(0)
        val startHour = 8
        val endHour = 22
        val nextChime = ChimeManager.calculateNextChimeTime(now, startHour, endHour)

        assertEquals("Hour should be the start hour", startHour, nextChime.hour)
        assertEquals("Minute should be 0", 0, nextChime.minute)
        assertEquals("Day should be tomorrow", now.plusDays(1).dayOfYear, nextChime.dayOfYear)
    }

    @Test
    fun calculateNextChimeTime_afterWindowLate_setsToStartHourTomorrow() {
        val now = ZonedDateTime.now().withHour(23).withMinute(45).withSecond(0).withNano(0)
        val startHour = 8
        val endHour = 22
        val nextChime = ChimeManager.calculateNextChimeTime(now, startHour, endHour)

        assertEquals("Hour should be the start hour", startHour, nextChime.hour)
        assertEquals("Minute should be 0", 0, nextChime.minute)
        assertEquals("Day should be tomorrow", now.plusDays(1).dayOfYear, nextChime.dayOfYear)
    }

    @Test
    fun calculateNextChimeTime_withinWindow_setsToNextHour() {
        val now = ZonedDateTime.now().withHour(13).withMinute(45).withSecond(0).withNano(0)
        val startHour = 8
        val endHour = 22
        val nextChime = ChimeManager.calculateNextChimeTime(now, startHour, endHour)

        assertEquals("Hour should be the next hour", 14, nextChime.hour)
        assertEquals("Minute should be 0", 0, nextChime.minute)
        assertEquals("Day should be the same", now.dayOfYear, nextChime.dayOfYear)
    }

    @Test
    fun chimeEnabled_getAndSet() {
        assertFalse(ChimeManager.isChimeEnabled(context)) // default

        ChimeManager.setChimeEnabled(context, true)
        assertTrue(ChimeManager.isChimeEnabled(context))

        ChimeManager.setChimeEnabled(context, false)
        assertFalse(ChimeManager.isChimeEnabled(context))
    }

    @Test
    fun startHour_getAndSet() {
        assertEquals(8, ChimeManager.getStartHour(context)) // default

        ChimeManager.setStartHour(context, 10)
        assertEquals(10, ChimeManager.getStartHour(context))
    }

    @Test
    fun endHour_getAndSet() {
        assertEquals(22, ChimeManager.getEndHour(context)) // default

        ChimeManager.setEndHour(context, 18)
        assertEquals(18, ChimeManager.getEndHour(context))
    }

    @Test
    fun scheduleNextChime_setsAlarm() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadowAlarmManager = shadowOf(alarmManager)
        
        ChimeManager.scheduleNextChime(context)
        
        val scheduledAlarms = shadowAlarmManager.nextScheduledAlarm
        assertTrue(scheduledAlarms != null)
    }

    @Test
    fun cancelChime_clearsAlarm() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadowAlarmManager = shadowOf(alarmManager)
        
        ChimeManager.scheduleNextChime(context)
        assertTrue(shadowAlarmManager.nextScheduledAlarm != null)
        
        ChimeManager.cancelChime(context)
        
        // Unfortunately shadowAlarmManager doesn't easily let us inspect all cancelled intents in all versions,
        // but we can check if the nextChimeTime is 0L
        assertEquals(0L, ChimeManager.getNextChimeTime(context))
    }
}
