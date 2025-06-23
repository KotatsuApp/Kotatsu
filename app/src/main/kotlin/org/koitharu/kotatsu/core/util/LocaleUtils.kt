package org.koitharu.kotatsu.core.util

import android.graphics.Paint
import androidx.core.graphics.PaintCompat
import org.koitharu.kotatsu.parsers.util.ifNullOrEmpty
import java.util.Locale

object LocaleUtils {

	private val paint = Paint()

	fun getEmojiFlag(locale: Locale): String? {
		val code = when (val c = locale.country.ifNullOrEmpty { locale.toLanguageTag() }.uppercase(Locale.ENGLISH)) {
			"EN" -> "GB"
			"JA" -> "JP"
			"VI" -> "VN"
			"ZH" -> "CN"
			"AR" -> "SA"
			else -> c
		}
		val emoji = countryCodeToEmojiFlag(code)
		return if (PaintCompat.hasGlyph(paint, emoji)) {
			emoji
		} else {
			null
		}
	}

	private fun countryCodeToEmojiFlag(countryCode: String): String {
		return countryCode.map { char ->
			Character.codePointAt("$char", 0) - 0x41 + 0x1F1E6
		}.map { codePoint ->
			Character.toChars(codePoint)
		}.joinToString(separator = "") { charArray ->
			String(charArray)
		}
	}
}
