package com.hosopy.actioncable

import com.squareup.okhttp.RequestBody
import com.squareup.okhttp.Response
import com.squareup.okhttp.ResponseBody
import com.squareup.okhttp.mockwebserver.MockResponse
import com.squareup.okhttp.mockwebserver.MockWebServer
import com.squareup.okhttp.ws.WebSocket
import com.squareup.okhttp.ws.WebSocketListener
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import okio.Buffer
import org.junit.Test
import java.io.IOException
import java.net.URI
import kotlin.test.assertEquals

private const val TIMEOUT = 10000L

class SubscriptionsTest {

    @Test(timeout = TIMEOUT)
    fun create() = runBlocking {
        val events = Channel<String>()

        val mockWebServer = MockWebServer()
        val mockResponse = MockResponse().withWebSocketUpgrade(object : DefaultWebSocketListener() {
            private var currentWebSocket: WebSocket? = null

            override fun onOpen(webSocket: WebSocket?, response: Response?) {
                currentWebSocket = webSocket
                // send welcome message
                launch(CommonPool) {
                    currentWebSocket?.sendMessage(RequestBody.create(WebSocket.TEXT, "{\"type\":\"welcome\"}"))
                }
            }

            override fun onMessage(message: ResponseBody?) {
                message?.also {
                    val text = it.source()?.readUtf8()!!
                    if (text.contains("subscribe")) {
                        // accept subscribe command
                        launch(CommonPool) {
                            currentWebSocket?.sendMessage(RequestBody.create(WebSocket.TEXT, "{\"identifier\":\"{\\\"channel\\\":\\\"CommentsChannel\\\"}\",\"type\":\"confirm_subscription\"}"))
                        }
                    }
                }?.close()
            }
        })
        mockWebServer.enqueue(mockResponse)
        mockWebServer.start()

        val channel = Channel("CommentsChannel")
        val consumer = Consumer(URI(mockWebServer.url("/").uri().toString()))

        consumer.connect()

        val subscription = consumer.subscriptions.create(channel)
        subscription.onConnected = {
            launch(CommonPool) {
                events.send("onConnected")
            }
        }

        assertEquals("onConnected", events.receive())

        mockWebServer.shutdown()
    }

    @Test(timeout = TIMEOUT)
    fun remove() = runBlocking {
        val events = Channel<String>()

        val mockWebServer = MockWebServer()
        val mockResponse = MockResponse().withWebSocketUpgrade(object : DefaultWebSocketListener() {
            private var currentWebSocket: WebSocket? = null

            override fun onOpen(webSocket: WebSocket?, response: Response?) {
                currentWebSocket = webSocket
                // send welcome message
                launch(CommonPool) {
                    currentWebSocket?.sendMessage(RequestBody.create(WebSocket.TEXT, "{\"type\":\"welcome\"}"))
                }
            }

            override fun onMessage(message: ResponseBody?) {
                message?.also {
                    val text = it.source()?.readUtf8()!!
                    if (text.contains("unsubscribe")) {
                        launch(CommonPool) {
                            events.send(text)
                        }
                    } else if (text.contains("subscribe")) {
                        // accept subscribe command
                        launch(CommonPool) {
                            currentWebSocket?.sendMessage(RequestBody.create(WebSocket.TEXT, "{\"identifier\":\"{\\\"channel\\\":\\\"CommentsChannel\\\"}\",\"type\":\"confirm_subscription\"}"))
                        }
                    }
                }?.close()
            }
        })
        mockWebServer.enqueue(mockResponse)
        mockWebServer.start()

        val consumer = Consumer(URI(mockWebServer.url("/").uri().toString()))
        val subscription1 = consumer.subscriptions.create(Channel("CommentsChannel"))
        val subscription2 = consumer.subscriptions.create(Channel("NotificationChannel"))

        subscription1.onConnected = {
            launch(CommonPool) {
                events.send("onConnected")
            }
        }

        subscription2.onConnected = {
            launch(CommonPool) {
                events.send("onConnected")
            }
        }

        consumer.connect()

        assertEquals("onConnected", events.receive())
        assertEquals("onConnected", events.receive())

        consumer.subscriptions.remove(subscription1)

        assertEquals(false, consumer.subscriptions.contains(subscription1))
        assertEquals(true, consumer.subscriptions.contains(subscription2))
        assertEquals("{\"command\":\"unsubscribe\",\"identifier\":\"{\\\"channel\\\":\\\"CommentsChannel\\\"}\"}", events.receive())

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
