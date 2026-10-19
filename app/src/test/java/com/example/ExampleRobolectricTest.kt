package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.Formatters
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Nova Player", appName)
    }

    @Test
    fun `test formatDuration`() {
        assertEquals("00:00", Formatters.formatDuration(0L))
        assertEquals("01:30", Formatters.formatDuration(90_000L))
        assertEquals("01:05:20", Formatters.formatDuration(3_920_000L))
    }

    @Test
    fun `test formatTimeDelta`() {
        assertEquals("+00:15", Formatters.formatTimeDelta(15_000L))
        assertEquals("-00:10", Formatters.formatTimeDelta(-10_000L))
    }
}
