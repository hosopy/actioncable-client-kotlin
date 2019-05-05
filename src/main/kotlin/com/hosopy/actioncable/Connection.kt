package com.hosopy.actioncable

import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import com.squareup.okhttp.RequestBody
import com.squareup.okhttp.Response
import com.squareup.okhttp.ResponseBody
import com.squareup.okhttp.ws.WebSocket
import com.squareup.okhttp.ws.WebSocketCall
import com.squareup.okhttp.ws.WebSocketListener
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.actor
import okio.Buffer
import java.io.IOException
import java.net.CookieHandler
import java.net.URI
import java.net.URLEncoder
import java.util.concurrent.Executors
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import kotlin.coroutines.CoroutineContext

typealias OkHttpClientFactory = () -> OkHttpClient


class Connection internal constructor(private val uri: URI, private val options: Options) {
    /**
     * Options for connection.
     *
     * @property sslContext SSLContext
     * @property hostnameVerifier HostnameVerifier
     * @property cookieHandler CookieHandler
     * @property query Query parameters to send on handshake.
     * @property headers HTTP Headers to send on handshake.
     * @property reconnection Whether to reconnect automatically. If reconnection is true, the client attempts to reconnect to the server when underlying connection is stale.
     * @property reconnectionMaxAttempts The maximum number of attempts to reconnect.
     * @property reconnectionDelay First delay seconds of reconnection.
     * @property reconnectionDelayMax Max delay seconds of reconnection.
     * @property okHttpClientFactory To use your own OkHttpClient, set this option.
     */
    data class Options(
            var sslContext: SSLContext? = null,
            var hostnameVerifier: HostnameVerifier? = null,
            var cookieHandler: CookieHandler? = null,
            var query: Map<String, String>? = null,
            var headers: Map<String, String>? = null,
            var reconnection: Boolean = false,
            var reconnectionMaxAttempts: Int = 30,
            var reconnectionDelay: Int = 3,
            var reconnectionDelayMax: Int = 30,
            var okHttpClientFactory: OkHttpClientFactory? = null
    )

    private enum class State {
        CONNECTING,
        OPEN,
        CLOSING,
        CLOSED
    }

    internal var onOpen: () -> Unit = {}
    internal var onMessage: (jsonString: String) -> Unit = {}
    internal var onClose: () -> Unit = {}
    internal var onFailure: (e: Exception) -> Unit = {}

    private var state = State.CONNECTING

    private var webSocket: WebSocket? = null
    
    @ObsoleteCoroutinesApi
    private val operationQueue = SerializedOperationQueue()

    private var isReopening = false

    @ObsoleteCoroutinesApi
    internal fun open() {
        operationQueue.push {
            if (isOpen()) {
                fireOnFailure(IllegalStateException("Must close existing connection before opening"))
            } else {
                doOpen()
            }
        }
    }

    @ObsoleteCoroutinesApi
    internal fun close() {
        operationQueue.push {
            webSocket?.let { webSocket ->
                if (!isState(State.CLOSING, State.CLOSED)) {
                    try {
                        webSocket.close(1000, "connection closed manually")
                        state = State.CLOSING
                    } catch (e: IOException) {
                        fireOnFailure(e)
                    } catch (e: IllegalStateException) {
                        fireOnFailure(e)
                    }
                }
            }
        }
    }

    @ObsoleteCoroutinesApi
    internal fun reopen() {
        if (isState(State.CLOSED)) {
            open()
        } else {
            isReopening = true
            close()
        }
    }

    @ObsoleteCoroutinesApi
    internal fun send(data: String): Boolean {
        if (!isOpen()) return false

        operationQueue.push {
            doSend(data)
        }

        return true
    }

    private fun isState(vararg states: State) = states.contains(state)

    private fun isOpen() = webSocket?.let { isState(State.OPEN) } ?: false

    @ObsoleteCoroutinesApi
    private fun doOpen() {
        state = State.CONNECTING

        val client = options.okHttpClientFactory?.invoke() ?: OkHttpClient()

        options.sslContext?.let { client.sslSocketFactory = it.socketFactory }
        options.hostnameVerifier?.let { client.hostnameVerifier = it }
        options.cookieHandler?.let { client.cookieHandler = it }

        val urlBuilder = StringBuilder(uri.toString())

        options.query?.let { urlBuilder.append("?${it.toQueryString()}") }

        val requestBuilder = Request.Builder().url(urlBuilder.toString())

        options.headers?.forEach { (key, value) -> requestBuilder.addHeader(key, value) }

        val request = requestBuilder.build()

        val webSocketCall = WebSocketCall.create(client, request)
        webSocketCall.enqueue(webSocketListener)

        client.dispatcher.executorService.shutdown()
    }

    private fun doSend(data: String) {
        webSocket?.let { webSocket ->
            try {
                webSocket.sendMessage(RequestBody.create(WebSocket.TEXT, data))
            } catch (e: IOException) {
                fireOnFailure(e)
            }
        }
    }

    private fun fireOnFailure(error: Exception) {
        onFailure.invoke(error)
    }

    @ObsoleteCoroutinesApi
    private val webSocketListener = object : WebSocketListener {
        override fun onOpen(openedWebSocket: WebSocket?, response: Response?) {
            state = State.OPEN
            webSocket = openedWebSocket
            operationQueue.push {
                onOpen.invoke()
            }
        }

        override fun onFailure(e: IOException?, response: Response?) {
            operationQueue.push {
                state = State.CLOSED
                onFailure.invoke(e ?: RuntimeException("Unexpected error"))
            }
        }

        override fun onMessage(message: ResponseBody?) {
            message?.also {
                it.source()?.readUtf8()?.also { text ->
                    operationQueue.push {
                        onMessage.invoke(text)
                    }
                }
            }?.close()
        }

        override fun onPong(payload: Buffer?) {}

        override fun onClose(code: Int, reason: String?) {
            println("WebSocket#onClose")
            state = State.CLOSED
            operationQueue.push {
                state = State.CLOSED

                onClose.invoke()

                if (isReopening) {
                    isReopening = false
                    open()
                }
            }
        }
    }
}

@ObsoleteCoroutinesApi
private class SerializedOperationQueue(capacity: Int = 0) : CoroutineScope {
    val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Unconfined + job

    private val singleThreadContext = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    private val actor = actor<suspend () -> Unit>(singleThreadContext, capacity) {
        for (operation in channel) {
            operation.invoke()
        }
    }

    fun push(operation: suspend () -> Unit) = launch {
        actor.send(operation)
    }
}

private fun Map<String, String>.toQueryString(): String {
    return this.keys.mapNotNull { key ->
        this[key]?.let {
            "$key=${URLEncoder.encode(this[key], Charsets.UTF_8.toString())}"
        }
    }.joinToString("&")
}
