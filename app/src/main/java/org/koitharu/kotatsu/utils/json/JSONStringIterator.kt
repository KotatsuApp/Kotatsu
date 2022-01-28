package org.koitharu.kotatsu.utils.json

import org.json.JSONArray

class JSONStringIterator(private val array: JSONArray) : Iterator<String> {

	private val total = array.length()
	private var index = 0

	override fun hasNext() = index < total

	override fun next(): String = array.getString(index++)
}