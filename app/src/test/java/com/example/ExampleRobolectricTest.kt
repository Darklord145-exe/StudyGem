package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.StudyMode
import com.example.data.model.SubjectType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
        assertEquals("Gemma Study", appName)
    }

    @Test
    fun `verify study modes and subjects`() {
        assertEquals("Teach", StudyMode.TEACH.title)
        assertEquals("Demonstrate", StudyMode.DEMONSTRATE.title)
        assertTrue(SubjectType.values().size >= 4)
    }
}
