package org.koitharu.kotatsu.ui.reader

import android.net.Uri
import androidx.annotation.LayoutRes
import org.koitharu.kotatsu.core.model.MangaPage
import org.koitharu.kotatsu.ui.common.BaseFragment

abstract class BaseReaderFragment(@LayoutRes contentLayoutId: Int) : BaseFragment(contentLayoutId), ReaderView {

	abstract val hasItems: Boolean

	abstract val currentPageIndex: Int

	abstract val pages: List<MangaPage>

	abstract fun setCurrentPage(index: Int, smooth: Boolean)

	val currentPage get() = pages.getOrNull(currentPageIndex)

	/**
	 * Handled by activity
	 */
	override fun onLoadingStateChanged(isLoading: Boolean) = Unit

	/**
	 * Handled by activity
	 */
	override fun onError(e: Exception) = Unit

	/**
	 * Handled by activity
	 */
	override fun onPageSaved(uri: Uri?) = Unit
}