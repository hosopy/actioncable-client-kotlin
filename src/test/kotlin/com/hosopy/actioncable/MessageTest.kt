package com.hosopy.actioncable

import com.beust.klaxon.JsonObject
import org.junit.Test
import kotlin.test.assertEquals

class MessageTest {
    @Test
    fun createFromJsonStringForWelcome() {
        val jsonString = "{\"type\":\"welcome\"}"
        val message = Message.createFromJsonString(jsonString)

        assertEquals(Message.Type.WELCOME, message?.type)
        assertEquals(null, message?.identifier)
        assertEquals(null, message?.body)
    }

    @Test
    fun createFromJsonStringForPing() {
        val jsonString = "{\"type\":\"ping\",\"message\":1505265037}"
        val message = Message.createFromJsonString(jsonString)

        assertEquals(Message.Type.PING, message?.type)
        assertEquals(null, message?.identifier)
        assertEquals(1505265037, message?.body)
    }

    @Test
    fun createFromJsonStringForConfirmation() {
        val jsonString = "{\"identifier\":\"{\\\"channel\\\":\\\"CommentsChannel\\\"}\",\"type\":\"confirm_subscription\"}"
        val message = Message.createFromJsonString(jsonString)

        assertEquals(Message.Type.CONFIRMATION, message?.type)
        assertEquals("{\"channel\":\"CommentsChannel\"}", message?.identifier)
        assertEquals(null, message?.body)
    }

    @Test
    fun createFromJsonStringForRejection() {
        val jsonString = "{\"identifier\":\"{\\\"channel\\\":\\\"CommentsChannel\\\"}\",\"type\":\"reject_subscription\"}"
        val message = Message.createFromJsonString(jsonString)

        assertEquals(Message.Type.REJECTION, message?.type)
        assertEquals("{\"channel\":\"CommentsChannel\"}", message?.identifier)
        assertEquals(null, message?.body)
    }
    
    @Test
    fun createFromJsonStringForMessage() {
        val jsonString = "{\"identifier\":\"{\\\"channel\\\":\\\"CommentsChannel\\\"}\",\"message\":{\"foo\":\"bar\"}}"
        val message = Message.createFromJsonString(jsonString)
        
        assertEquals(Message.Type.MESSAGE, message?.type)
        assertEquals("{\"channel\":\"CommentsChannel\"}", message?.identifier)
        assertEquals(JsonObject(mapOf("foo" to "bar")), message?.body)
    }
    
    @Test
    fun createFromJsonStringForString() {
        val jsonString = "{\"identifier\":\"{\\\"channel\\\":\\\"CommentsChannel\\\"}\",\"message\": \"bar\"}"
        val message = Message.createFromJsonString(jsonString)
        
        assertEquals(Message.Type.MESSAGE, message?.type)
        assertEquals("{\"channel\":\"CommentsChannel\"}", message?.identifier)
        assertEquals("bar", message?.body)
    }
}
