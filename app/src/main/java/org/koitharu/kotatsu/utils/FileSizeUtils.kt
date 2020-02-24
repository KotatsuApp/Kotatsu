package org.koitharu.kotatsu.utils

import android.content.Context
import org.koitharu.kotatsu.R
import java.text.DecimalFormat
import kotlin.math.log10
import kotlin.math.pow


object FileSizeUtils {

	@JvmStatic
	fun mbToBytes(mb: Int) = 1024L * 1024L * mb

	@JvmStatic
	fun kbToBytes(kb: Int) = 1024L * kb

	@JvmStatic
	fun formatBytes(context: Context, bytes: Long): String {
		val units = context.getString(R.string.text_file_sizes).split('|')
		if (bytes <= 0) {
			return "0 ${units.first()}"
		}
		val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt()
		return buildString {
			append(
				DecimalFormat("#,##0.#").format(
					bytes / 1024.0.pow(digitGroups.toDouble())
				).toString()
			)
			append(' ')
			append(units.getOrNull(digitGroups).orEmpty())
		}
	}
}