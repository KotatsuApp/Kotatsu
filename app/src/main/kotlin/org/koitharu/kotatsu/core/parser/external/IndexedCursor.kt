package org.koitharu.kotatsu.core.parser.external

import android.database.Cursor
import android.database.CursorWrapper
import androidx.collection.MutableObjectIntMap
import androidx.collection.ObjectIntMap
import org.koitharu.kotatsu.core.util.ext.getBoolean

class IndexedCursor(cursor: Cursor) : CursorWrapper(cursor) {

	private val columns: ObjectIntMap<String> = MutableObjectIntMap<String>(columnCount).also { result ->
		val names = columnNames
		names.forEachIndexed { index, s -> result.put(s, index) }
	}

	fun getString(columnName: String): String {
		return getString(columns[columnName])
	}

	fun getStringOrNull(columnName: String): String? {
		val columnIndex = columns.getOrDefault(columnName, -1)
		return when {
			columnIndex == -1 -> null
			isNull(columnIndex) -> null
			else -> getString(columnIndex)
		}
	}

	fun getBoolean(columnName: String): Boolean {
		return getBoolean(columns[columnName])
	}

	fun getBooleanOrDefault(columnName: String, defaultValue: Boolean): Boolean {
		val columnIndex = columns.getOrDefault(columnName, -1)
		return when {
			columnIndex == -1 -> defaultValue
			isNull(columnIndex) -> defaultValue
			else -> getBoolean(columnIndex)
		}
	}

	fun getInt(columnName: String): Int {
		return getInt(columns[columnName])
	}

	fun getIntOrDefault(columnName: String, defaultValue: Int): Int {
		val columnIndex = columns.getOrDefault(columnName, -1)
		return when {
			columnIndex == -1 -> defaultValue
			isNull(columnIndex) -> defaultValue
			else -> getInt(columnIndex)
		}
	}

	fun getLong(columnName: String): Long {
		return getLong(columns[columnName])
	}

	fun getLongOrDefault(columnName: String, defaultValue: Long): Long {
		val columnIndex = columns.getOrDefault(columnName, -1)
		return when {
			columnIndex == -1 -> defaultValue
			isNull(columnIndex) -> defaultValue
			else -> getLong(columnIndex)
		}
	}

	fun getFloat(columnName: String): Float {
		return getFloat(columns[columnName])
	}

	fun getFloatOrDefault(columnName: String, defaultValue: Float): Float {
		val columnIndex = columns.getOrDefault(columnName, -1)
		return when {
			columnIndex == -1 -> defaultValue
			isNull(columnIndex) -> defaultValue
			else -> getFloat(columnIndex)
		}
	}
}
