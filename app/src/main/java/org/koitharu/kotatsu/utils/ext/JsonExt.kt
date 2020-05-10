package org.koitharu.kotatsu.utils.ext

import org.json.JSONArray
import org.json.JSONObject

fun <T> JSONArray.map(block: (JSONObject) -> T): List<T> {
	val len = length()
	val result = ArrayList<T>(len)
	for (i in 0 until len) {
		val jo = getJSONObject(i)
		result.add(block(jo))
	}
	return result
}

fun <T> JSONArray.mapIndexed(block: (Int, JSONObject) -> T): List<T> {
	val len = length()
	val result = ArrayList<T>(len)
	for (i in 0 until len) {
		val jo = getJSONObject(i)
		result.add(block(i, jo))
	}
	return result
}

fun JSONObject.getStringOrNull(name: String): String? = opt(name)?.toString()

operator fun JSONArray.iterator(): Iterator<JSONObject> = JSONIterator(this)

private class JSONIterator(private val array: JSONArray) : Iterator<JSONObject> {

	private val total = array.length()
	private var index = 0

	override fun hasNext() = index < total - 1

	override fun next(): JSONObject = array.getJSONObject(index++)

}