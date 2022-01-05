package org.koitharu.kotatsu.utils.json

import org.json.JSONObject

class JSONValuesIterator(
	private val jo: JSONObject,
): Iterator<Any> {

	private val keyIterator = jo.keys()

	override fun hasNext(): Boolean = keyIterator.hasNext()

	override fun next(): Any {
		val key = keyIterator.next()
		return jo.get(key)
	}
}