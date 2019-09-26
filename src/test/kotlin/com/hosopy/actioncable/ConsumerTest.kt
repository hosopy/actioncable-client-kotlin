package com.hosopy.actioncable

import com.squareup.okhttp.Response
import com.squareup.okhttp.ResponseBody
import com.squareup.okhttp.mockwebserver.MockResponse
import com.squareup.okhttp.mockwebserver.MockWebServer
import com.squareup.okhttp.ws.WebSocket
import com.squareup.okhttp.ws.WebSocketListener
import kotlinx.coroutines.Dispatchers.Unconfined
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okio.Buffer
import org.junit.Test
import java.io.IOException
import java.net.URI
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

private const val TIMEOUT = 10000L

class ConsumerTest {
    @Test
    fun createWithValidUri() {
        val consumer = Consumer(URI("ws://example.com:28080"))
        assertNotNull(consumer)
    }

    @Test
    fun createWithValidUriAndOptions() {
        val consumer = Consumer(URI("ws://example.com:28080"), Consumer.Options())
        assertNotNull(consumer)
    }

    @Test
    fun subscriptions() {
        val consumer = Consumer(URI("ws://example.com:28080"))
        assertNotNull(consumer.subscriptions)
    }

    @Test(timeout = TIMEOUT)
    fun connect() = runBlocking {
        val events = Channel<String>()

        val mockWebServer = MockWebServer()
        val mockResponse = MockResponse().withWebSocketUpgrade(object : DefaultWebSocketListener() {
            override fun onOpen(webSocket: WebSocket?, response: Response?) {
                GlobalScope.launch(Unconfined) {
                    events.send("onOpen")
                }
            }
        })
        mockWebServer.enqueue(mockResponse)
        mockWebServer.start()

        val consumer = ActionCable.createConsumer(URI(mockWebServer.url("/").uri().toString()))
        consumer.connect()

        assertEquals("onOpen", events.receive())

        mockWebServer.shutdown()
    }

    @Test(timeout = TIMEOUT)
    fun disconnect() = runBlocking {
        val events = Channel<String>()

        val mockWebServer = MockWebServer()
        val mockResponse = MockResponse().withWebSocketUpgrade(object : DefaultWebSocketListener() {
            override fun onOpen(webSocket: WebSocket?, response: Response?) {
                launch(Unconfined) {
                    events.send("onOpen")
                }
            }

            override fun onClose(code: Int, reason: String?) {
                launch(Unconfined) {
                    events.send("onClose")
                }
            }
        })
        mockWebServer.enqueue(mockResponse)
        mockWebServer.start()

        val consumer = ActionCable.createConsumer(URI(mockWebServer.url("/").uri().toString()))
        consumer.connect()

        assertEquals("onOpen", events.receive())

        consumer.disconnect()

        assertEquals("onClose", events.receive())

        mockWebServer.shutdown()
    }

    @Test(timeout = TIMEOUT)
    fun send() = runBlocking {
        val events = Channel<String>()

        val mockWebServer = MockWebServer()
        val mockResponse = MockResponse().withWebSocketUpgrade(object : DefaultWebSocketListener() {
            override fun onOpen(webSocket: WebSocket?, response: Response?) {
                launch(Unconfined) {
                    events.send("onOpen")
                }
            }

            override fun onMessage(message: ResponseBody?) {
                message?.also {
                    it.source()?.readUtf8()?.also { text ->
                        launch(Unconfined) {
                            events.send("onMessage:$text")
                        }
                    }
                }?.close()
            }
        })
        mockWebServer.enqueue(mockResponse)
        mockWebServer.start()

        val consumer = ActionCable.createConsumer(URI(mockWebServer.url("/").uri().toString()))
        consumer.connect()

        assertEquals("onOpen", events.receive())

        consumer.send(Command.subscribe("identifier"))

        assertEquals("onMessage:{\"command\":\"subscribe\",\"identifier\":\"identifier\"}", events.receive())

        mockWebServer.shutdown()
    }


    private open class DefaultWebSocketListener : WebSocketListener {
        override fun onOpen(webSocket: WebSocket?, response: Response?) {
        }

        override fun onFailure(e: IOException?, response: Response?) {
        }

        override fun onMessage(message: ResponseBody?) {
        }

        override fun onPong(payload: Buffer?) {
        }

        override fun onClose(code: Int, reason: String?) {
        }
    }
}
