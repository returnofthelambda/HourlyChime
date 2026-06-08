package com.example.hourlychime

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZonedDateTime

class ChimeManagerTest {

    @Test
    fun calculateNextChimeTime_beforeWindow_setsToStartHourToday() {
        // Current time: 6:30 AM
        val now = ZonedDateTime.now().withHour(6).withMinute(30).withSecond(0).withNano(0)

        // Window: 8 AM to 10 PM
        val startHour = 8
        val endHour = 22

        val nextChime = ChimeManager.calculateNextChimeTime(now, startHour, endHour)

        // Expected: 8:00 AM today
        assertEquals("Hour should be the start hour", startHour, nextChime.hour)
        assertEquals("Minute should be 0", 0, nextChime.minute)
        assertEquals("Second should be 0", 0, nextChime.second)
        assertEquals("Nano should be 0", 0, nextChime.nano)
        assertEquals("Day should be the same", now.dayOfYear, nextChime.dayOfYear)
    }

    @Test
    fun calculateNextChimeTime_afterWindow_setsToStartHourTomorrow() {
        // Current time: 10:15 PM
        val now = ZonedDateTime.now().withHour(22).withMinute(15).withSecond(0).withNano(0)
        val today = now.dayOfYear

        // Window: 8 AM to 10 PM
        val startHour = 8
        val endHour = 22

        val nextChime = ChimeManager.calculateNextChimeTime(now, startHour, endHour)

        // Expected: 8:00 AM tomorrow
        assertEquals("Hour should be the start hour", startHour, nextChime.hour)
        assertEquals("Minute should be 0", 0, nextChime.minute)
        assertEquals("Second should be 0", 0, nextChime.second)
        assertEquals("Nano should be 0", 0, nextChime.nano)

        val expectedDay = now.plusDays(1).dayOfYear
        assertEquals("Day should be tomorrow", expectedDay, nextChime.dayOfYear)
    }

    @Test
    fun calculateNextChimeTime_afterWindowLate_setsToStartHourTomorrow() {
        // Current time: 11:45 PM
        val now = ZonedDateTime.now().withHour(23).withMinute(45).withSecond(0).withNano(0)
        val today = now.dayOfYear

        // Window: 8 AM to 10 PM
        val startHour = 8
        val endHour = 22

        val nextChime = ChimeManager.calculateNextChimeTime(now, startHour, endHour)

        // Expected: 8:00 AM tomorrow
        assertEquals("Hour should be the start hour", startHour, nextChime.hour)
        assertEquals("Minute should be 0", 0, nextChime.minute)
        assertEquals("Second should be 0", 0, nextChime.second)
        assertEquals("Nano should be 0", 0, nextChime.nano)

        val expectedDay = now.plusDays(1).dayOfYear
        assertEquals("Day should be tomorrow", expectedDay, nextChime.dayOfYear)
    }

    @Test
    fun calculateNextChimeTime_withinWindow_setsToNextHour() {
        // Current time: 1:45 PM (13:45)
        val now = ZonedDateTime.now().withHour(13).withMinute(45).withSecond(0).withNano(0)

        // Window: 8 AM to 10 PM
        val startHour = 8
        val endHour = 22

        val nextChime = ChimeManager.calculateNextChimeTime(now, startHour, endHour)

        // Expected: 2:00 PM (14:00) today
        assertEquals("Hour should be the next hour", 14, nextChime.hour)
        assertEquals("Minute should be 0", 0, nextChime.minute)
        assertEquals("Second should be 0", 0, nextChime.second)
        assertEquals("Nano should be 0", 0, nextChime.nano)
        assertEquals("Day should be the same", now.dayOfYear, nextChime.dayOfYear)
    }
}
