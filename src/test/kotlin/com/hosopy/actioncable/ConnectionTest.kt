package com.hosopy.actioncable

import com.squareup.okhttp.RequestBody
import com.squareup.okhttp.Response
import com.squareup.okhttp.ResponseBody
import com.squareup.okhttp.mockwebserver.MockResponse
import com.squareup.okhttp.mockwebserver.MockWebServer
import com.squareup.okhttp.ws.WebSocket
import com.squareup.okhttp.ws.WebSocketListener
import kotlinx.coroutines.Dispatchers.Unconfined
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

class ConnectionTest {
    @Test
    fun createUriAndOptions() {
        val connection = Connection(URI("ws://example.com:28080"), Connection.Options())
        assertNotNull(connection)
    }

    @Test(timeout = TIMEOUT)
    fun onOpen() = runBlocking {
        val events = Channel<String>()

        val mockWebServer = MockWebServer()
        val mockResponse = MockResponse().withWebSocketUpgrade(DefaultWebSocketListener())
        mockWebServer.enqueue(mockResponse)
        mockWebServer.start()

        val connection = Connection(URI(mockWebServer.url("/").uri().toString()), Connection.Options())
        connection.onOpen = {
            launch(Unconfined) {
                events.send("onOpen")
            }
        }
        connection.open()

        assertEquals("onOpen", events.receive())

        mockWebServer.shutdown()
    }

    @Test(timeout = TIMEOUT)
    fun onMessage() = runBlocking {
        val events = Channel<String>()

        val mockWebServer = MockWebServer()
        val mockResponse = MockResponse().withWebSocketUpgrade(object : DefaultWebSocketListener() {
            override fun onOpen(webSocket: WebSocket?, response: Response?) {
                webSocket?.sendMessage(RequestBody.create(WebSocket.TEXT, "{}"))
            }
        })
        mockWebServer.enqueue(mockResponse)
        mockWebServer.start()

        val connection = Connection(URI(mockWebServer.url("/").uri().toString()), Connection.Options())
        connection.onMessage = { text ->
            launch(Unconfined) {
                events.send("onMessage:$text")
            }

        }
        connection.open()

        assertEquals("onMessage:{}", events.receive())

        mockWebServer.shutdown()
    }

    @Test
    fun onCloseWhenDisconnectedByClient() = runBlocking {
        val events = Channel<String>()

        val mockWebServer = MockWebServer()
        val mockResponse = MockResponse().withWebSocketUpgrade(DefaultWebSocketListener())
        mockWebServer.enqueue(mockResponse)
        mockWebServer.start()

        val connection = Connection(URI(mockWebServer.url("/").uri().toString()), Connection.Options())
        connection.onOpen = {
            launch(Unconfined) {
                events.send("onOpen")
            }
        }
        connection.onClose = {
            launch(Unconfined) {
                events.send("onClose")
            }
        }
        connection.open()

        assertEquals("onOpen", events.receive())

        connection.close()

        assertEquals("onClose", events.receive())

        mockWebServer.shutdown()
    }

    @Test
    fun onCloseWhenDisconnectedByServer() = runBlocking {
        val events = Channel<String>()

        val mockWebServer = MockWebServer()
        val mockResponse = MockResponse().withWebSocketUpgrade(object : DefaultWebSocketListener() {
            override fun onOpen(webSocket: WebSocket?, response: Response?) {
                webSocket?.close(1000, "Reason")
            }
        })
        mockWebServer.enqueue(mockResponse)
        mockWebServer.start()

        val connection = Connection(URI(mockWebServer.url("/").uri().toString()), Connection.Options())
        connection.onClose = {
            launch(Unconfined) {
                events.send("onClose")
            }
        }
        connection.open()

        assertEquals("onClose", events.receive())

        mockWebServer.shutdown()
    }

    @Test
    fun onFailureWhenInternalServerErrorReceived() = runBlocking {
        val events = Channel<String>()

        val mockWebServer = MockWebServer()
        val mockResponse = MockResponse()
        mockResponse.setResponseCode(500)
        mockResponse.status = "HTTP/1.1 500 Internal Server Error"
        mockWebServer.enqueue(mockResponse)
        mockWebServer.start()

        val connection = Connection(URI(mockWebServer.url("/").uri().toString()), Connection.Options())
        connection.onFailure = {
            launch(Unconfined) {
                events.send("onFailure")
            }
        }
        connection.open()

        assertEquals("onFailure", events.receive())

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
