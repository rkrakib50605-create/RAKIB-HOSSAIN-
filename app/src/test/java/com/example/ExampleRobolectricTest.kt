package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.CommandType
import com.example.service.AiBrainService
import com.example.service.CommandDispatcher
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("RAKIBX9 JARVIS", appName)
    }

    @Test
    fun `verify time command returns valid time format for Rakib`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dispatcher = CommandDispatcher(context, AiBrainService())
        val result = dispatcher.processCommand("what time is it", ownerName = "Rakib")

        assertEquals(CommandType.TIME, result.commandType)
        assertTrue(result.replyText.contains("Rakib"))
        assertTrue(result.replyText.contains("time is"))
    }

    @Test
    fun `verify date command returns valid date format for Rakib`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dispatcher = CommandDispatcher(context, AiBrainService())
        val result = dispatcher.processCommand("what is today's date", ownerName = "Rakib")

        assertEquals(CommandType.DATE, result.commandType)
        assertTrue(result.replyText.contains("Rakib"))
        assertTrue(result.replyText.contains("date is"))
    }

    @Test
    fun `verify youtube command opens youtube`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dispatcher = CommandDispatcher(context, AiBrainService())
        val result = dispatcher.processCommand("open youtube", ownerName = "Rakib")

        assertEquals(CommandType.YOUTUBE, result.commandType)
        assertEquals("https://www.youtube.com", result.actionUrl)
        assertTrue(result.replyText.contains("YouTube"))
    }

    @Test
    fun `verify google command opens google`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dispatcher = CommandDispatcher(context, AiBrainService())
        val result = dispatcher.processCommand("open google", ownerName = "Rakib")

        assertEquals(CommandType.GOOGLE, result.commandType)
        assertEquals("https://www.google.com", result.actionUrl)
        assertTrue(result.replyText.contains("Google"))
    }

    @Test
    fun `verify exit command signals exit`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dispatcher = CommandDispatcher(context, AiBrainService())
        val result = dispatcher.processCommand("goodbye jarvis", ownerName = "Rakib")

        assertEquals(CommandType.EXIT, result.commandType)
        assertTrue(result.shouldExit)
        assertTrue(result.replyText.contains("Goodbye Rakib"))
    }

    @Test
    fun `verify math calculation query returns valid evaluation`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dispatcher = CommandDispatcher(context, AiBrainService())
        val result = dispatcher.processCommand("what is 25 * 4", ownerName = "Rakib")

        assertEquals(CommandType.AI_CHAT, result.commandType)
        assertTrue(result.replyText.contains("100"))
    }

    @Test
    fun `verify bangla conversational response`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dispatcher = CommandDispatcher(context, AiBrainService())
        val result = dispatcher.processCommand("কেমন আছো", ownerName = "Rakib")

        assertEquals(CommandType.AI_CHAT, result.commandType)
        assertTrue(result.replyText.contains("রাকিব ভাই"))
    }
}
