package com.hosopy.actioncable

import com.beust.klaxon.JsonObject

/**
 * Channel is a descriptor of a channel.
 *
 * ```kotlin
 * val appearanceChannel = Channel("AppearanceChannel");
 *
 * // With parameter
 * val chatChannel = Channel("AppearanceChannel", mapOf(
 *     "room" to "Best Room",
 *     "maxPeople" to 5,
 *     "isPrivate" to true
 * ))
 * ```
 *
 * @author hosopy <https://github.com/hosopy>
 */
data class Channel(val channel: String, private val params: Map<String, Any?> = mapOf()) {

    internal val identifier: String

    init {
        require(!params.containsKey("channel")) { "channel is a reserved key" }

        val json = JsonObject(params)
        json["channel"] = channel
        identifier = json.toJsonString()
    }
}
