package org.koitharu.kotatsu.core.util.ext

import android.content.ContentValues
import android.database.Cursor
import org.json.JSONObject

fun Cursor.toJson(): JSONObject {
	val jo = JSONObject()
	for (i in 0 until columnCount) {
		val name = getColumnName(i)
		when (getType(i)) {
			Cursor.FIELD_TYPE_STRING -> jo.put(name, getString(i))
			Cursor.FIELD_TYPE_FLOAT -> jo.put(name, getDouble(i))
			Cursor.FIELD_TYPE_INTEGER -> jo.put(name, getLong(i))
			Cursor.FIELD_TYPE_NULL -> jo.put(name, null)
			Cursor.FIELD_TYPE_BLOB -> jo.put(name, getBlob(i))
		}
	}
	return jo
}

fun JSONObject.toContentValues(): ContentValues {
	val cv = ContentValues(length())
	for (key in keys()) {
		val name = key.escapeName()
		when (val value = get(key)) {
			JSONObject.NULL, "null", null -> cv.putNull(name)
			is String -> cv.put(name, value)
			is Float -> cv.put(name, value)
			is Double -> cv.put(name, value)
			is Int -> cv.put(name, value)
			is Long -> cv.put(name, value)
			else -> throw IllegalArgumentException("Value $value cannot be putted in ContentValues")
		}
	}
	return cv
}

private fun String.escapeName() = "`$this`"

fun Cursor.getBoolean(columnIndex: Int) = getInt(columnIndex) > 0
