package org.koitharu.kotatsu.core.exceptions

import org.json.JSONArray
import org.koitharu.kotatsu.utils.ext.map

class GraphQLException(private val errors: JSONArray) : RuntimeException() {

	val messages = errors.map {
		it.getString("message")
	}

	override val message: String
		get() = messages.joinToString("\n")
}