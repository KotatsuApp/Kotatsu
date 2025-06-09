package org.koitharu.kotatsu.core.util.ext

import android.content.ContentValues
import android.database.Cursor
import androidx.collection.ArraySet

fun Cursor.getBoolean(columnIndex: Int) = getInt(columnIndex) > 0

inline fun <T> Cursor.map(mapper: (Cursor) -> T): List<T> = mapTo(ArrayList(count), mapper)

inline fun <T> Cursor.mapToSet(mapper: (Cursor) -> T): Set<T> = mapTo(ArraySet(count), mapper)

inline fun <T, C: MutableCollection<in T>> Cursor.mapTo(destination: C, mapper: (Cursor) -> T): C = use { c ->
	if (c.moveToFirst()) {
		do {
			destination.add(mapper(c))
		} while (c.moveToNext())
	}
	destination
}

inline fun buildContentValues(capacity: Int, block: ContentValues.() -> Unit): ContentValues {
	return ContentValues(capacity).apply(block)
}
