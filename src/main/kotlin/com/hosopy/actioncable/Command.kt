package com.hosopy.actioncable

import com.beust.klaxon.JsonObject
import com.beust.klaxon.json

internal data class Command(private val command: String, private val identifier: String, private val data: Map<String, Any?> = mapOf()) {

    companion object {
        fun subscribe(identifier: String) = Command("subscribe", identifier)
        fun unsubscribe(identifier: String) = Command("unsubscribe", identifier)
        fun message(identifier: String, data: Map<String, Any?>) = Command("message", identifier, data)
    }

    fun toJsonString(): String {
        return if (data.isEmpty()) {
            json {
                obj("command" to command, "identifier" to identifier)
            }.toJsonString()
        } else {
            json {
                obj("command" to command, "identifier" to identifier, "data" to JsonObject(data).toJsonString())
            }.toJsonString()
        }
    }
}
