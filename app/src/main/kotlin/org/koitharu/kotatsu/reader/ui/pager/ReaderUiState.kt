package org.koitharu.kotatsu.reader.ui.pager

import android.content.Context
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.parsers.util.format

data class ReaderUiState(
	val mangaName: String?,
	val branch: String?,
	val chapterName: String?,
	val chapterNumber: Int,
	val chaptersTotal: Int,
	val currentPage: Int,
	val totalPages: Int,
	val percent: Float,
	val incognito: Boolean,
	private val isSliderEnabled: Boolean,
) {

	fun isSliderAvailable(): Boolean {
		return isSliderEnabled && totalPages > 1 && currentPage < totalPages
	}

	fun resolveSubtitle(context: Context): String? {
		val firstPart = branch
		val secondPart = if (incognito) {
			context.getString(R.string.incognito_mode)
		} else if (percent in 0f..1f) {
			context.getString(R.string.percent_string_pattern, (percent * 100).format())
		} else {
			null
		}
		return if (firstPart != null && secondPart != null) {
			context.getString(R.string.download_summary_pattern, firstPart, secondPart)
		} else {
			firstPart ?: secondPart
		}
	}

	fun resolveSummary(context: Context) = context.getString(
		R.string.reader_info_pattern,
		chapterNumber,
		chaptersTotal,
		currentPage + 1,
		totalPages,
	)
}
