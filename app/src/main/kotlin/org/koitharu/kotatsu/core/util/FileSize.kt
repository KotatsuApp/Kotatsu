package org.koitharu.kotatsu.core.util

import android.content.Context
import org.koitharu.kotatsu.R
import java.text.DecimalFormat
import kotlin.math.log10
import kotlin.math.pow

enum class FileSize(private val multiplier: Int) {

	BYTES(1), KILOBYTES(1024), MEGABYTES(1024 * 1024);

	fun convert(amount: Long, target: FileSize): Long = amount * multiplier / target.multiplier

	fun format(context: Context, amount: Long): String {
		val bytes = amount * multiplier
		val units = context.getString(R.string.text_file_sizes).split('|')
		if (bytes <= 0) {
			return "0 ${units.first()}"
		}
		val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt()
		return buildString {
			append(
				DecimalFormat("#,##0.#").format(
					bytes / 1024.0.pow(digitGroups.toDouble()),
				),
			)
			val unit = units.getOrNull(digitGroups)
			if (unit != null) {
				append(' ')
				append(unit)
			}
		}
	}
}
