package com.example.hourlychime

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class BootReceiverTest {

    private lateinit var context: Context
    private lateinit var receiver: BootReceiver

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        receiver = BootReceiver()
        mockkObject(ChimeManager)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun onReceive_bootCompleted_chimeEnabled_schedulesChime() {
        every { ChimeManager.isChimeEnabled(context) } returns true
        every { ChimeManager.scheduleNextChime(context) } returns Unit

        val intent = Intent(Intent.ACTION_BOOT_COMPLETED)
        receiver.onReceive(context, intent)

        verify { ChimeManager.scheduleNextChime(context) }
    }

    @Test
    fun onReceive_bootCompleted_chimeDisabled_doesNothing() {
        every { ChimeManager.isChimeEnabled(context) } returns false

        val intent = Intent(Intent.ACTION_BOOT_COMPLETED)
        receiver.onReceive(context, intent)

        verify(exactly = 0) { ChimeManager.scheduleNextChime(context) }
    }

    @Test
    fun onReceive_otherAction_doesNothing() {
        val intent = Intent(Intent.ACTION_BATTERY_LOW)
        receiver.onReceive(context, intent)

        verify(exactly = 0) { ChimeManager.isChimeEnabled(context) }
    }
}
