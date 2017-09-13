package com.hosopy.actioncable

import java.net.URI

object ActionCable {
    /**
     * Create an actioncable consumer.
     *
     * @param uri URI to connect
     * @param options Options for consumer.
     * @see com.hosopy.actioncable.Consumer.Options
     * @return Consumer
     *
     * ```
     * val uri = URI("ws://localhost:28080")
     * val options = Consumer.Options()
     *
     * val consumer = ActionCable.createConsumer(uri, options)
     * ```
     */
    fun createConsumer(uri: URI, options: Consumer.Options = Consumer.Options()): Consumer {
        return Consumer(uri, options)
    }
}
