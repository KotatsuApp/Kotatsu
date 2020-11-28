package org.koitharu.kotatsu.reader.ui.reversed

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.fragment_reader_standard.*
import org.koin.android.ext.android.inject
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.reader.ui.ReaderState
import org.koitharu.kotatsu.reader.ui.base.AbstractReader
import org.koitharu.kotatsu.reader.ui.base.BaseReaderAdapter
import org.koitharu.kotatsu.reader.ui.base.ReaderPage
import org.koitharu.kotatsu.reader.ui.standard.PageAnimTransformer
import org.koitharu.kotatsu.reader.ui.standard.PagerPaginationListener
import org.koitharu.kotatsu.utils.ext.doOnPageChanged
import org.koitharu.kotatsu.utils.ext.swapAdapter
import org.koitharu.kotatsu.utils.ext.withArgs

class ReversedReaderFragment : AbstractReader(R.layout.fragment_reader_standard),
	SharedPreferences.OnSharedPreferenceChangeListener {

	private var paginationListener: PagerPaginationListener? = null
	private val settings by inject<AppSettings>()

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		paginationListener = PagerPaginationListener(adapter!!, 2, this)
		pager.adapter = adapter
		if (settings.readerAnimation) {
			pager.setPageTransformer(ReversedPageAnimTransformer())
		}
		pager.offscreenPageLimit = 2
		pager.registerOnPageChangeCallback(paginationListener!!)
		pager.doOnPageChanged {
			notifyPageChanged(reversed(it))
		}
	}

	override fun onAttach(context: Context) {
		super.onAttach(context)
		settings.subscribe(this)
	}

	override fun onDetach() {
		settings.unsubscribe(this)
		super.onDetach()
	}

	override fun onDestroyView() {
		paginationListener = null
		super.onDestroyView()
	}

	override fun onCreateAdapter(dataSet: List<ReaderPage>): BaseReaderAdapter {
		return ReversedPagesAdapter(dataSet, loader)
	}

	override fun recreateAdapter() {
		super.recreateAdapter()
		pager.swapAdapter(adapter)
	}

	override fun getCurrentItem() = reversed(pager.currentItem)

	override fun setCurrentItem(position: Int, isSmooth: Boolean) {
		pager.setCurrentItem(reversed(position), isSmooth)
	}

	override fun getCurrentPageScroll() = 0

	override fun restorePageScroll(position: Int, scroll: Int) = Unit

	override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
		when (key) {
			AppSettings.KEY_READER_ANIMATION -> {
				if (settings.readerAnimation) {
					pager.setPageTransformer(PageAnimTransformer())
				} else {
					pager.setPageTransformer(null)
				}
			}
		}
	}

	override fun getLastPage() = pages.firstOrNull()

	override fun getFirstPage() = pages.lastOrNull()

	private fun reversed(position: Int) = (itemsCount - position - 1).coerceAtLeast(0)

	companion object {

		fun newInstance(state: ReaderState) = ReversedReaderFragment().withArgs(1) {
			putParcelable(ARG_STATE, state)
		}
	}
}