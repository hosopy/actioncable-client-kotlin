package com.hosopy.actioncable

/**
 * Collection class for creating (and internally managing) channel subscriptions.
 *
 * ```
 * // Create a subscription instance
 * val subscription = consumer.subscriptions.create(appearanceChannel)
 *
 * // Remove a subscription instance
 * consumer.subscriptions.remove(subscription)
 * ```
 */
class Subscriptions internal constructor(private val consumer: Consumer) {

    private val subscriptions = mutableListOf<Subscription>()

    /**
     * Create [Subscription] instance.
     *
     * @param channel Channel to connect
     * @return Subscription instance
     */
    fun create(channel: Channel): Subscription = Subscription(consumer, channel).also {
        subscriptions.add(it)
    }

    /**
     * Remove subscription from collection.
     *
     * @param subscription instance to remove
     */
    fun remove(subscription: Subscription) {
        if (subscriptions.remove(subscription)) {
            consumer.send(Command.unsubscribe(subscription.identifier))
        }
    }

    fun contains(subscription: Subscription) = subscriptions.contains(subscription)

    internal fun reload() {
        subscriptions.forEach { sendSubscribeCommand(it) }
    }

    internal fun notifyConnected(identifier: String) {
        subscriptions.filter { it.identifier == identifier }.forEach { it.notifyConnected() }
    }

    internal fun notifyDisconnected() {
        subscriptions.forEach { it.notifyDisconnected() }
    }

    internal fun notifyReceived(identifier: String, data: Any?) {
        subscriptions.filter { it.identifier == identifier }.forEach { it.notifyReceived(data) }
    }

    internal fun notifyFailed(error: Exception) {
        subscriptions.forEach { it.notifyFailed(error) }
    }

    internal fun reject(identifier: String) {
        val removal = subscriptions.filter { it.identifier == identifier }
        subscriptions.removeAll(removal)
        removal.forEach { it.notifyRejected() }
    }

    private fun sendSubscribeCommand(subscription: Subscription) {
        consumer.send(Command.subscribe(subscription.identifier))
    }
}
