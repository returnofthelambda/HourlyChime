package com.example.hourlychime

import android.app.AlarmManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class PomodoroManagerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // Mock TileService to avoid InflateException or errors when updating tile from tests
        mockkStatic(androidx.wear.tiles.TileService::class)
        every { androidx.wear.tiles.TileService.getUpdater(any()) } throws Exception("Mocked TileService")
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun defaultState_isIdle() {
        assertEquals(PomodoroManager.State.IDLE, PomodoroManager.getState(context))
        assertEquals(0L, PomodoroManager.getEndTime(context))
    }

    @Test
    fun startSession_work_updatesStateAndEndTime() {
        PomodoroManager.setWorkDuration(context, 25)
        
        PomodoroManager.startSession(context, PomodoroManager.State.WORK)

        assertEquals(PomodoroManager.State.WORK, PomodoroManager.getState(context))
        val endTime = PomodoroManager.getEndTime(context)
        assertNotEquals(0L, endTime)
        
        // End time should be roughly now + 25 minutes
        val expectedMin = System.currentTimeMillis() + (24 * 60 * 1000L)
        val expectedMax = System.currentTimeMillis() + (26 * 60 * 1000L)
        assert(endTime in expectedMin..expectedMax)
    }

    @Test
    fun startSession_break_updatesStateAndEndTime() {
        PomodoroManager.setBreakDuration(context, 5)
        
        PomodoroManager.startSession(context, PomodoroManager.State.BREAK)

        assertEquals(PomodoroManager.State.BREAK, PomodoroManager.getState(context))
        val endTime = PomodoroManager.getEndTime(context)
        assertNotEquals(0L, endTime)
    }

    @Test
    fun stopSession_clearsStateAndEndTime() {
        PomodoroManager.startSession(context, PomodoroManager.State.WORK)
        
        PomodoroManager.stopSession(context)

        assertEquals(PomodoroManager.State.IDLE, PomodoroManager.getState(context))
        assertEquals(0L, PomodoroManager.getEndTime(context))
    }

    @Test
    fun getSetWorkDuration() {
        assertEquals(25, PomodoroManager.getWorkDuration(context)) // Default
        
        PomodoroManager.setWorkDuration(context, 45)
        assertEquals(45, PomodoroManager.getWorkDuration(context))
    }

    @Test
    fun getSetBreakDuration() {
        assertEquals(5, PomodoroManager.getBreakDuration(context)) // Default
        
        PomodoroManager.setBreakDuration(context, 15)
        assertEquals(15, PomodoroManager.getBreakDuration(context))
    }
}
