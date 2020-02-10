package org.koitharu.kotatsu.ui.main.list.local

import kotlinx.android.synthetic.main.fragment_list.*
import moxy.ktx.moxyPresenter
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.ui.main.list.MangaListFragment
import java.io.File

class LocalListFragment : MangaListFragment<File>() {

	private val presenter by moxyPresenter(factory = ::LocalListPresenter)

	override fun onRequestMoreItems(offset: Int) {
		if (offset == 0) {
			presenter.loadList()
		}
	}

	override fun getTitle(): CharSequence? {
		return getString(R.string.local_storage)
	}

	override fun setUpEmptyListHolder() {
		textView_holder.setText(R.string.no_saved_manga)
		textView_holder.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
	}

	companion object {

		fun newInstance() = LocalListFragment()
	}
}