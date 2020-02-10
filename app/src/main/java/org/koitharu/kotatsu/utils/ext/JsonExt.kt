package org.koitharu.kotatsu.utils.ext

import org.json.JSONArray
import org.json.JSONObject

fun <T> JSONArray.map(block: (JSONObject) -> T): List<T> {
	val len = length()
	val result = ArrayList<T>(len)
	for(i in 0 until len) {
		val jo = getJSONObject(i)
		result.add(block(jo))
	}
	return result
}