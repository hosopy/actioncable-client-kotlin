package com.hosopy.actioncable

import org.junit.Test
import java.net.URI
import kotlin.test.assertNotNull

class ActionCableTest {
    @Test
    fun createWithUri() {
        val consumer = ActionCable.createConsumer(URI("ws://example.com:2808"))
        assertNotNull(consumer)
    }

    @Test
    fun createWithUriAndOptions() {
        val consumer = ActionCable.createConsumer(URI("ws://example.com:2808"), Consumer.Options())
        assertNotNull(consumer)
    }
}
