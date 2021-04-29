package org.koitharu.kotatsu.utils.ext

import androidx.collection.ArraySet
import org.json.JSONArray
import org.json.JSONObject

inline fun <R, C : MutableCollection<in R>> JSONArray.mapTo(
	destination: C,
	block: (JSONObject) -> R
): C {
	val len = length()
	for (i in 0 until len) {
		val jo = getJSONObject(i)
		destination.add(block(jo))
	}
	return destination
}

inline fun <T> JSONArray.map(block: (JSONObject) -> T): List<T> {
	return mapTo(ArrayList(length()), block)
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

fun JSONObject.getStringOrNull(name: String): String? = opt(name)?.takeUnless {
	it === JSONObject.NULL
}?.toString()

fun JSONObject.getBooleanOrDefault(name: String, defaultValue: Boolean): Boolean = opt(name)?.takeUnless {
	it === JSONObject.NULL
} as? Boolean ?: defaultValue

operator fun JSONArray.iterator(): Iterator<JSONObject> = JSONIterator(this)

private class JSONIterator(private val array: JSONArray) : Iterator<JSONObject> {

	private val total = array.length()
	private var index = 0

	override fun hasNext() = index < total - 1

	override fun next(): JSONObject = array.getJSONObject(index++)

}

fun <T> JSONArray.mapToSet(block: (JSONObject) -> T): Set<T> {
	val len = length()
	val result = ArraySet<T>(len)
	for (i in 0 until len) {
		val jo = getJSONObject(i)
		result.add(block(jo))
	}
	return result
}