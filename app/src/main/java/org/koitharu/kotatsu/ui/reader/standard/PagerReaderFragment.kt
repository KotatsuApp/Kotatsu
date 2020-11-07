package org.koitharu.kotatsu.ui.reader.standard

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.fragment_reader_standard.*
import org.koin.android.ext.android.inject
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.ui.reader.ReaderState
import org.koitharu.kotatsu.ui.reader.base.AbstractReader
import org.koitharu.kotatsu.ui.reader.base.BaseReaderAdapter
import org.koitharu.kotatsu.ui.reader.base.ReaderPage
import org.koitharu.kotatsu.utils.ext.doOnPageChanged
import org.koitharu.kotatsu.utils.ext.swapAdapter
import org.koitharu.kotatsu.utils.ext.withArgs

class PagerReaderFragment : AbstractReader(R.layout.fragment_reader_standard),
	SharedPreferences.OnSharedPreferenceChangeListener {

	private var paginationListener: PagerPaginationListener? = null
	private val settings by inject<AppSettings>()

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		paginationListener = PagerPaginationListener(adapter!!, 2, this)
		pager.adapter = adapter
		if (settings.readerAnimation) {
			pager.setPageTransformer(PageAnimTransformer())
		}
		pager.offscreenPageLimit = 2
		pager.registerOnPageChangeCallback(paginationListener!!)
		pager.doOnPageChanged(::notifyPageChanged)
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
		return PagesAdapter(dataSet, loader)
	}

	override fun recreateAdapter() {
		super.recreateAdapter()
		pager.swapAdapter(adapter)
	}

	override fun getCurrentItem() = pager.currentItem

	override fun setCurrentItem(position: Int, isSmooth: Boolean) {
		pager.setCurrentItem(position, isSmooth)
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

	companion object {

		fun newInstance(state: ReaderState) = PagerReaderFragment().withArgs(1) {
			putParcelable(ARG_STATE, state)
		}
	}
}