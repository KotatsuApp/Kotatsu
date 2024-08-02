package org.koitharu.kotatsu.core.parser.external

import android.database.Cursor
import android.database.CursorWrapper
import org.koitharu.kotatsu.core.util.ext.getBoolean

class SafeCursor(cursor: Cursor) : CursorWrapper(cursor) {

	fun getString(columnName: String): String {
		return getString(getColumnIndexOrThrow(columnName))
	}

	fun getStringOrNull(columnName: String): String? {
		val columnIndex = getColumnIndex(columnName)
		return when {
			columnIndex < 0 -> null
			isNull(columnIndex) -> null
			else -> getString(columnIndex)
		}
	}

	fun getBoolean(columnName: String): Boolean {
		return getBoolean(getColumnIndexOrThrow(columnName))
	}

	fun getBooleanOrDefault(columnName: String, defaultValue: Boolean): Boolean {
		val columnIndex = getColumnIndex(columnName)
		return when {
			columnIndex < 0 -> defaultValue
			isNull(columnIndex) -> defaultValue
			else -> getBoolean(columnIndex)
		}
	}

	fun getInt(columnName: String): Int {
		return getInt(getColumnIndexOrThrow(columnName))
	}

	fun getIntOrDefault(columnName: String, defaultValue: Int): Int {
		val columnIndex = getColumnIndex(columnName)
		return when {
			columnIndex < 0 -> defaultValue
			isNull(columnIndex) -> defaultValue
			else -> getInt(columnIndex)
		}
	}

	fun getLong(columnName: String): Long {
		return getLong(getColumnIndexOrThrow(columnName))
	}

	fun getLongOrDefault(columnName: String, defaultValue: Long): Long {
		val columnIndex = getColumnIndex(columnName)
		return when {
			columnIndex < 0 -> defaultValue
			isNull(columnIndex) -> defaultValue
			else -> getLong(columnIndex)
		}
	}

	fun getFloat(columnName: String): Float {
		return getFloat(getColumnIndexOrThrow(columnName))
	}

	fun getFloatOrDefault(columnName: String, defaultValue: Float): Float {
		val columnIndex = getColumnIndex(columnName)
		return when {
			columnIndex < 0 -> defaultValue
			isNull(columnIndex) -> defaultValue
			else -> getFloat(columnIndex)
		}
	}
}
