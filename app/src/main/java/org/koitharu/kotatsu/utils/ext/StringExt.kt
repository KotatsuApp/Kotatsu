package org.koitharu.kotatsu.utils.ext

fun String.longHashCode(): Long {
	var h = 1125899906842597L
	val len: Int = this.length
	for (i in 0 until len) {
		h = 31 * h + this[i].toLong()
	}
	return h
}

fun String.withDomain(domain: String, ssl: Boolean = true) = when {
	this.startsWith("/") -> buildString {
		append("http")
		if (ssl) {
			append('s')
		}
		append("://")
		append(domain)
		append(this@withDomain)
	}
	else -> this
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
			val p = cyr.binarySearch(c)
			if (p in lat.indices) {
				append(lat[p])
			} else if (!skipMissing) {
				append(c)
			}
		}
	}
}

fun String.toFileName() = this.transliterate(false)
	.replace(Regex("[^a-z0-9_\\-]", setOf(RegexOption.IGNORE_CASE)), " ")
	.replace(Regex("\\s+"), "_")