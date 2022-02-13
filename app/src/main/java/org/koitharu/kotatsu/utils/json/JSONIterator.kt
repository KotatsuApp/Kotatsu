package org.koitharu.kotatsu.utils.json

import org.json.JSONArray
import org.json.JSONObject

class JSONIterator(private val array: JSONArray) : Iterator<JSONObject> {

	private val total = array.length()
	private var index = 0

	override fun hasNext() = index < total

	override fun next(): JSONObject = array.getJSONObject(index++)
}