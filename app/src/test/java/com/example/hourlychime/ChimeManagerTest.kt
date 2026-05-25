package com.example.hourlychime

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class ChimeManagerTest {

    @Test
    fun calculateNextChimeTime_beforeWindow_setsToStartHourToday() {
        // Current time: 6:30 AM
        val now = Calendar.getInstance()
        now.set(Calendar.HOUR_OF_DAY, 6)
        now.set(Calendar.MINUTE, 30)

        // Window: 8 AM to 10 PM
        val startHour = 8
        val endHour = 22

        val nextChime = ChimeManager.calculateNextChimeTime(now, startHour, endHour)

        // Expected: 8:00 AM today
        assertEquals("Hour should be the start hour", startHour, nextChime.get(Calendar.HOUR_OF_DAY))
        assertEquals("Minute should be 0", 0, nextChime.get(Calendar.MINUTE))
        assertEquals("Second should be 0", 0, nextChime.get(Calendar.SECOND))
        assertEquals("Millisecond should be 0", 0, nextChime.get(Calendar.MILLISECOND))
        assertEquals("Day should be the same", now.get(Calendar.DAY_OF_YEAR), nextChime.get(Calendar.DAY_OF_YEAR))
    }

    @Test
    fun calculateNextChimeTime_afterWindow_setsToStartHourTomorrow() {
        // Current time: 10:15 PM
        val now = Calendar.getInstance()
        now.set(Calendar.HOUR_OF_DAY, 22)
        now.set(Calendar.MINUTE, 15)
        val today = now.get(Calendar.DAY_OF_YEAR)

        // Window: 8 AM to 10 PM
        val startHour = 8
        val endHour = 22

        val nextChime = ChimeManager.calculateNextChimeTime(now, startHour, endHour)

        // Expected: 8:00 AM tomorrow
        assertEquals("Hour should be the start hour", startHour, nextChime.get(Calendar.HOUR_OF_DAY))
        assertEquals("Minute should be 0", 0, nextChime.get(Calendar.MINUTE))
        assertEquals("Second should be 0", 0, nextChime.get(Calendar.SECOND))
        assertEquals("Millisecond should be 0", 0, nextChime.get(Calendar.MILLISECOND))

        // Handling end of year wrap-around for tests
        val expectedDay = if (today == now.getActualMaximum(Calendar.DAY_OF_YEAR)) 1 else today + 1
        assertEquals("Day should be tomorrow", expectedDay, nextChime.get(Calendar.DAY_OF_YEAR))
    }

    @Test
    fun calculateNextChimeTime_afterWindowLate_setsToStartHourTomorrow() {
        // Current time: 11:45 PM
        val now = Calendar.getInstance()
        now.set(Calendar.HOUR_OF_DAY, 23)
        now.set(Calendar.MINUTE, 45)
        val today = now.get(Calendar.DAY_OF_YEAR)

        // Window: 8 AM to 10 PM
        val startHour = 8
        val endHour = 22

        val nextChime = ChimeManager.calculateNextChimeTime(now, startHour, endHour)

        // Expected: 8:00 AM tomorrow
        assertEquals("Hour should be the start hour", startHour, nextChime.get(Calendar.HOUR_OF_DAY))
        assertEquals("Minute should be 0", 0, nextChime.get(Calendar.MINUTE))
        assertEquals("Second should be 0", 0, nextChime.get(Calendar.SECOND))
        assertEquals("Millisecond should be 0", 0, nextChime.get(Calendar.MILLISECOND))

        // Handling end of year wrap-around for tests
        val expectedDay = if (today == now.getActualMaximum(Calendar.DAY_OF_YEAR)) 1 else today + 1
        assertEquals("Day should be tomorrow", expectedDay, nextChime.get(Calendar.DAY_OF_YEAR))
    }

    @Test
    fun calculateNextChimeTime_withinWindow_setsToNextHour() {
        // Current time: 1:45 PM (13:45)
        val now = Calendar.getInstance()
        now.set(Calendar.HOUR_OF_DAY, 13)
        now.set(Calendar.MINUTE, 45)

        // Window: 8 AM to 10 PM
        val startHour = 8
        val endHour = 22

        val nextChime = ChimeManager.calculateNextChimeTime(now, startHour, endHour)

        // Expected: 2:00 PM (14:00) today
        assertEquals("Hour should be the next hour", 14, nextChime.get(Calendar.HOUR_OF_DAY))
        assertEquals("Minute should be 0", 0, nextChime.get(Calendar.MINUTE))
        assertEquals("Second should be 0", 0, nextChime.get(Calendar.SECOND))
        assertEquals("Millisecond should be 0", 0, nextChime.get(Calendar.MILLISECOND))
        assertEquals("Day should be the same", now.get(Calendar.DAY_OF_YEAR), nextChime.get(Calendar.DAY_OF_YEAR))
    }
}
