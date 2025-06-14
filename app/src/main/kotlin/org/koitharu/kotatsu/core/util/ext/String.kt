package org.koitharu.kotatsu.core.util.ext

import android.content.Context
import androidx.collection.arraySetOf
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.parsers.util.ellipsize
import org.koitharu.kotatsu.parsers.util.nullIfEmpty
import java.util.UUID

fun String.toUUIDOrNull(): UUID? = try {
	UUID.fromString(this)
} catch (e: IllegalArgumentException) {
	e.printStackTraceDebug()
	null
}

fun String.transliterate(skipMissing: Boolean): String {
	val cyr = charArrayOf(
		'а', 'б', 'в', 'г', 'д', 'е', 'ж', 'з', 'и', 'й', 'к', 'л', 'м', 'н', 'о', 'п',
		'р', 'с', 'т', 'у', 'ф', 'х', 'ц', 'ч', 'ш', 'щ', 'ъ', 'ы', 'ь', 'э', 'ю', 'я', 'ё', 'ў',
	)
	val lat = arrayOf(
		"a", "b", "v", "g", "d", "e", "zh", "z", "i", "y", "k", "l", "m", "n", "o", "p",
		"r", "s", "t", "u", "f", "h", "ts", "ch", "sh", "sch", "", "i", "", "e", "ju", "ja", "jo", "w",
	)
	return buildString(length + 5) {
		for (c in this@transliterate) {
			val p = cyr.binarySearch(c.lowercaseChar())
			if (p in lat.indices) {
				if (c.isUpperCase()) {
					append(lat[p].uppercase())
				} else {
					append(lat[p])
				}
			} else if (!skipMissing) {
				append(c)
			}
		}
	}
}

fun String.toFileNameSafe(): String = this.transliterate(false)
	.replace(Regex("[^a-z0-9_\\-]", arraySetOf(RegexOption.IGNORE_CASE)), " ")
	.replace(Regex("\\s+"), "_")

fun CharSequence.sanitize(): CharSequence {
	return filterNot { c -> c.isReplacement() }
}

fun Char.isReplacement() = this in '\uFFF0'..'\uFFFF'

fun <T> Collection<T>.joinToStringWithLimit(context: Context, limit: Int, transform: ((T) -> String)): String {
	if (size == 1) {
		return transform(first()).ellipsize(limit)
	}
	return buildString(limit + 6) {
		for ((i, item) in this@joinToStringWithLimit.withIndex()) {
			val str = transform(item)
			when {
				i == 0 -> append(str.ellipsize(limit - 4))
				length + str.length > limit -> {
					append(", ")
					append(context.getString(R.string.list_ellipsize_pattern, this@joinToStringWithLimit.size - i))
					break
				}

				else -> append(", ").append(str)
			}
		}
	}
}

fun String.isHttpUrl() = startsWith("https://", ignoreCase = true) || startsWith("http://", ignoreCase = true)

fun concatStrings(context: Context, a: String?, b: String?): String? = when {
	a.isNullOrEmpty() && b.isNullOrEmpty() -> null
	a.isNullOrEmpty() -> b?.nullIfEmpty()
	b.isNullOrEmpty() -> a.nullIfEmpty()
	else -> context.getString(R.string.download_summary_pattern, a, b)
}
