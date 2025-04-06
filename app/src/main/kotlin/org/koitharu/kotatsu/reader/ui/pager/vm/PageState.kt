package org.koitharu.kotatsu.reader.ui.pager.vm

import com.davemorrissey.labs.subscaleview.ImageSource

sealed class PageState {

	data object Empty : PageState()

	data class Loading(
		val preview: ImageSource?,
		val progress: Int,
	) : PageState()

	data class Loaded(
		val source: ImageSource,
		val isConverted: Boolean,
	) : PageState()

	class Converting() : PageState()

	data class Shown(
		val source: ImageSource,
		val isConverted: Boolean,
	) : PageState()

	data class Error(
		val error: Throwable,
	) : PageState()

	fun isFinalState(): Boolean = this is Error || this is Shown
}
