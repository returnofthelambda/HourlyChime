package com.example.hourlychime

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.LocalTime

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class AlarmReceiverTest {

    private lateinit var context: Context
    private lateinit var receiver: AlarmReceiver

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        receiver = AlarmReceiver()
        mockkObject(ChimeManager)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun onReceive_testChimeAction_showsNotification() {
        val intent = Intent(AlarmReceiver.ACTION_TEST_CHIME)
        
        receiver.onReceive(context, intent)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val shadowNotificationManager = shadowOf(notificationManager)
        
        assertEquals(1, shadowNotificationManager.allNotifications.size)
        val notification = shadowNotificationManager.allNotifications[0]
        
        assertEquals("Tempo Chime", shadowOf(notification).contentTitle)
    }

    @Test
    fun onReceive_pomodoroDoneAction_showsPomodoroNotification() {
        val intent = Intent(AlarmReceiver.ACTION_POMODORO_DONE)
        
        receiver.onReceive(context, intent)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val shadowNotificationManager = shadowOf(notificationManager)
        
        assertEquals(1, shadowNotificationManager.allNotifications.size)
        val notification = shadowNotificationManager.allNotifications[0]
        
        assertEquals("Focus Timer", shadowOf(notification).contentTitle)
        assertEquals("Session finished", shadowOf(notification).contentText)
    }

    @Test
    fun onReceive_hourlyChimeAction_chimeDisabled_doesNothing() {
        every { ChimeManager.isChimeEnabled(context) } returns false

        val intent = Intent(AlarmReceiver.ACTION_HOURLY_CHIME)
        receiver.onReceive(context, intent)

        verify(exactly = 0) { ChimeManager.scheduleNextChime(context) }
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val shadowNotificationManager = shadowOf(notificationManager)
        assertEquals(0, shadowNotificationManager.allNotifications.size)
    }

    @Test
    fun onReceive_hourlyChimeAction_chimeEnabled_schedulesNext() {
        every { ChimeManager.isChimeEnabled(context) } returns true
        every { ChimeManager.scheduleNextChime(context) } returns Unit
        every { ChimeManager.getStartHour(context) } returns 0
        every { ChimeManager.getEndHour(context) } returns 23
        every { ChimeManager.setLastChimeTime(context, any()) } returns Unit

        val intent = Intent(AlarmReceiver.ACTION_HOURLY_CHIME)
        receiver.onReceive(context, intent)

        verify { ChimeManager.scheduleNextChime(context) }
    }
}
