package org.koitharu.kotatsu.utils.ext

import android.net.Uri
import androidx.collection.arraySetOf
import java.math.BigInteger
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.*
import kotlin.math.min

fun String.longHashCode(): Long {
	var h = 1125899906842597L
	val len: Int = this.length
	for (i in 0 until len) {
		h = 31 * h + this[i].toLong()
	}
	return h
}

fun String.removeSurrounding(vararg chars: Char): String {
	if (length == 0) {
		return this
	}
	for (c in chars) {
		if (first() == c && last() == c) {
			return substring(1, length - 1)
		}
	}
	return this
}

fun String.transliterate(skipMissing: Boolean): String {
	val cyr = charArrayOf(
		'a', 'б', 'в', 'г', 'д', 'ё', 'ж', 'з', 'и', 'к', 'л', 'м', 'н',
		'п', 'р', 'с', 'т', 'у', 'ў', 'ф', 'х', 'ц', 'ш', 'щ', 'ы', 'э', 'ю', 'я'
	)
	val lat = arrayOf(
		"a", "b", "v", "g", "d", "jo", "zh", "z", "i", "k", "l", "m", "n",
		"p", "r", "s", "t", "u", "w", "f", "h", "ts", "sh", "sch", "", "e", "ju", "ja"
	)
	return buildString(length + 5) {
		for (c in this@transliterate) {
			val p = cyr.binarySearch(c.toLowerCase())
			if (p in lat.indices) {
				append(lat[p])
			} else if (!skipMissing) {
				append(c)
			}
		}
	}
}

fun String.toFileNameSafe() = this.transliterate(false)
	.replace(Regex("[^a-z0-9_\\-]", arraySetOf(RegexOption.IGNORE_CASE)), " ")
	.replace(Regex("\\s+"), "_")

fun String.ellipsize(maxLength: Int) = if (this.length > maxLength) {
	this.take(maxLength - 1) + Typography.ellipsis
} else this

fun String.urlEncoded(): String = URLEncoder.encode(this, Charsets.UTF_8.name())

fun String.toUriOrNull(): Uri? = if (isEmpty()) {
	null
} else {
	Uri.parse(this)
}

fun ByteArray.byte2HexFormatted(): String {
	val str = StringBuilder(size * 2)
	for (i in indices) {
		var h = Integer.toHexString(this[i].toInt())
		val l = h.length
		if (l == 1) {
			h = "0$h"
		}
		if (l > 2) {
			h = h.substring(l - 2, l)
		}
		str.append(h.toUpperCase(Locale.ROOT))
		if (i < size - 1) {
			str.append(':')
		}
	}
	return str.toString()
}

fun String.md5(): String {
	val md = MessageDigest.getInstance("MD5")
	return BigInteger(1, md.digest(toByteArray()))
		.toString(16)
		.padStart(32, '0')
}

fun String.substringBetween(from: String, to: String, fallbackValue: String): String {
	val fromIndex = indexOf(from)
	if (fromIndex == -1) {
		return fallbackValue
	}
	val toIndex = lastIndexOf(to)
	return if (toIndex == -1) {
		fallbackValue
	} else {
		substring(fromIndex + from.length, toIndex)
	}
}

fun String.find(regex: Regex) = regex.find(this)?.value

fun String.levenshteinDistance(other: String): Int {
	if (this == other) {
		return 0
	}
	if (this.isEmpty()) {
		return other.length
	}
	if (other.isEmpty()) {
		return this.length
	}

	val lhsLength = this.length + 1
	val rhsLength = other.length + 1

	var cost = Array(lhsLength) { it }
	var newCost = Array(lhsLength) { 0 }

	for (i in 1 until rhsLength) {
		newCost[0] = i

		for (j in 1 until lhsLength) {
			val match = if (this[j - 1] == other[i - 1]) 0 else 1

			val costReplace = cost[j - 1] + match
			val costInsert = cost[j] + 1
			val costDelete = newCost[j - 1] + 1

			newCost[j] = min(min(costInsert, costDelete), costReplace)
		}

		val swap = cost
		cost = newCost
		newCost = swap
	}

	return cost[lhsLength - 1]
}