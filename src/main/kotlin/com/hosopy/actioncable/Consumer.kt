package com.hosopy.actioncable

import kotlinx.coroutines.ObsoleteCoroutinesApi
import java.net.URI

/**
 * The Consumer establishes the connection to a server-side Ruby Connection object.
 * Once established, the ConnectionMonitor will ensure that its properly maintained through heartbeats and checking for stale updates.
 * The Consumer instance is also the gateway to establishing subscriptions to desired channels.
 *
 * @property subscriptions Subscriptions container.
 *
 * ```
 * val consumer = ActionCable.createConsumer(uri, options)
 * val appearanceChannel = Channel("AppearanceChannel")
 * val subscription = consumer.subscriptions.create(appearanceChannel)
 * ```
 */
class Consumer internal constructor(uri: URI, options: Options = Options()) {
    /**
     * Consumer options.
     *
     * @property connection Connection options.
     * @see com.hosopy.actioncable.Connection.Options
     *
     * ```
     * val options = Consumer.Options()
     * options.connection.reconnection = true
     * options.connection.query = mapOf("user_id" to "1")
     * ```
     */
    data class Options(val connection: Connection.Options = Connection.Options())

    val subscriptions: Subscriptions = Subscriptions(this)

    private val connection: Connection = Connection(uri, options.connection)

    private val connectionMonitor: ConnectionMonitor = ConnectionMonitor(connection, options.connection)

    init {
        connection.onOpen = {
        
        }

        connection.onMessage = { jsonString ->
            Message.createFromJsonString(jsonString)?.let {
                (type, identifier, body) ->
                when (type) {
                    Message.Type.WELCOME -> {
                        connectionMonitor.recordConnect()
                        subscriptions.reload()
                    }
                    Message.Type.PING -> connectionMonitor.recordPing()
                    Message.Type.CONFIRMATION -> subscriptions.notifyConnected(identifier!!)
                    Message.Type.REJECTION -> subscriptions.reject(identifier!!)
                    Message.Type.MESSAGE -> subscriptions.notifyReceived(identifier!!, body)
                }
            }
        }

        connection.onClose = {
            subscriptions.notifyDisconnected()
            connectionMonitor.recordDisconnect()
        }

        connection.onFailure = { error ->
            subscriptions.notifyFailed(error)
        }
    }

    /**
     * Establish connection.
     */
    @ObsoleteCoroutinesApi
    fun connect() {
        connection.open()
        connectionMonitor.start()
    }

    /**
     * Disconnect the underlying connection.
     */
    @ObsoleteCoroutinesApi
    fun disconnect() {
        connection.close()
        connectionMonitor.stop()
    }

    @ObsoleteCoroutinesApi
    internal fun send(command: Command) = connection.send(command.toJsonString())
}
