package org.koitharu.kotatsu.reader.ui.pager

import android.content.Context
import org.koitharu.kotatsu.R

data class ReaderUiState(
	val mangaName: String?,
	val branch: String?,
	val chapterName: String?,
	val chapterNumber: Int,
	val chaptersTotal: Int,
	val currentPage: Int,
	val totalPages: Int,
	val percent: Float,
	private val isSliderEnabled: Boolean,
) {

	fun isSliderAvailable(): Boolean {
		return isSliderEnabled && totalPages > 1 && currentPage < totalPages
	}

	fun resolveTitle(context: Context): String? = when {
		mangaName == null -> null
		branch == null -> mangaName
		else -> context.getString(R.string.manga_branch_title_template, mangaName, branch)
	}
}
