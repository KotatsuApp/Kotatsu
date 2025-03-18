package org.koitharu.kotatsu.core.parser.external

import android.database.Cursor
import android.database.CursorWrapper
import org.koitharu.kotatsu.core.exceptions.IncompatiblePluginException
import org.koitharu.kotatsu.core.util.ext.getBoolean

class ExternalPluginCursor(private val source: ExternalMangaSource, cursor: Cursor) : CursorWrapper(cursor) {

	override fun getColumnIndexOrThrow(columnName: String?): Int = try {
		super.getColumnIndexOrThrow(columnName)
	} catch (e: Exception) {
		throw IncompatiblePluginException(source.name, e)
	}

	fun getString(columnName: String): String = getString(getColumnIndexOrThrow(columnName))

	fun getStringOrNull(columnName: String): String? {
		val columnIndex = getColumnIndex(columnName)
		return when {
			columnIndex < 0 -> null
			isNull(columnIndex) -> null
			else -> getString(columnIndex).takeUnless { it == "null" }
		}
	}

	fun getBoolean(columnName: String): Boolean = getBoolean(getColumnIndexOrThrow(columnName))

	fun getBooleanOrDefault(columnName: String, defaultValue: Boolean): Boolean {
		val columnIndex = getColumnIndex(columnName)
		return when {
			columnIndex < 0 -> defaultValue
			isNull(columnIndex) -> defaultValue
			else -> getBoolean(columnIndex)
		}
	}

	fun getInt(columnName: String): Int = getInt(getColumnIndexOrThrow(columnName))

	fun getIntOrDefault(columnName: String, defaultValue: Int): Int {
		val columnIndex = getColumnIndex(columnName)
		return when {
			columnIndex < 0 -> defaultValue
			isNull(columnIndex) -> defaultValue
			else -> getInt(columnIndex)
		}
	}

	fun getLong(columnName: String): Long = getLong(getColumnIndexOrThrow(columnName))

	fun getLongOrDefault(columnName: String, defaultValue: Long): Long {
		val columnIndex = getColumnIndex(columnName)
		return when {
			columnIndex < 0 -> defaultValue
			isNull(columnIndex) -> defaultValue
			else -> getLong(columnIndex)
		}
	}

	fun getFloat(columnName: String): Float = getFloat(getColumnIndexOrThrow(columnName))

	fun getFloatOrDefault(columnName: String, defaultValue: Float): Float {
		val columnIndex = getColumnIndex(columnName)
		return when {
			columnIndex < 0 -> defaultValue
			isNull(columnIndex) -> defaultValue
			else -> getFloat(columnIndex)
		}
	}
}
