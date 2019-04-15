package com.hosopy.actioncable

import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

private const val STALE_THRESHOLD = 6

internal class ConnectionMonitor(private val connection: Connection, private val options: Connection.Options) : CoroutineScope {
    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + job

    private var pingedAt = 0L

    private var disconnectedAt = 0L

    private var startedAt = 0L

    private var stoppedAt = 0L

    private var reconnectAttempts = 0

    private val interval: Long
        get() {
            return Math.max(
                    options.reconnectionDelay, Math.min(
                    options.reconnectionDelayMax,
                    (5.0 * Math.log((reconnectAttempts + 1).toDouble())).toInt()
            )
            ) * 1000L
        }

    private val connectionIsStale: Boolean
        get() = secondsSince(if (pingedAt > 0) pingedAt else startedAt) > STALE_THRESHOLD

    private val disconnectedRecently: Boolean
        get() = disconnectedAt != 0L && secondsSince(disconnectedAt) < STALE_THRESHOLD

    internal fun recordConnect() {
        reset()
        pingedAt = now()
        disconnectedAt = 0L
    }

    internal fun recordDisconnect() {
        disconnectedAt = now()
    }

    internal fun recordPing() {
        pingedAt = now()
    }

    internal fun start() {
        reset()
        stoppedAt = 0L
        startedAt = now()
        poll()
    }

    internal fun stop() {
        job.cancel()
        stoppedAt = now()
    }

    private fun poll() {
        launch {
            delay(interval)
            while (true) {
                if (stoppedAt == 0L) {
                    reconnectIfStale()
                }
                delay(interval)
            }
        }
    }

    private fun reset() {

        reconnectAttempts = 0
    }

    private fun now(): Long = System.currentTimeMillis()

    private fun reconnectIfStale() {
        if (options.reconnection && connectionIsStale && reconnectAttempts < options.reconnectionMaxAttempts) {
            reconnectAttempts++
            if (!disconnectedRecently) {
                connection.reopen()
            }
        }
    }

    private fun secondsSince(time: Long): Long = (now() - time) / 1000
}
