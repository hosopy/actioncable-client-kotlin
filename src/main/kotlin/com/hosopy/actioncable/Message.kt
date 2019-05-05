package com.hosopy.actioncable

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.google.gson.JsonParser


internal data class Message(val type: Type, val identifier: String?, val body: Any?) {
	
	enum class Type(val typeString: String?) {
		WELCOME("welcome"),
		PING("ping"),
		CONFIRMATION("confirm_subscription"),
		REJECTION("reject_subscription"),
		MESSAGE(null);
	}
	
	companion object {
		private val parser: Parser = Parser.default()
		
		fun createFromJsonString(jsonString: String): Message? {
			return (parser.parse(StringBuilder(jsonString)) as JsonObject?)?.let { json ->
				val type = json.string("type")?.let { typeString ->
					Type.values().first { it.typeString == typeString }
				} ?: Type.MESSAGE
				val identifier = json.string("identifier")
				val body = json["message"]
				
				Message(type, identifier, body)
			}
		}
	}
}
