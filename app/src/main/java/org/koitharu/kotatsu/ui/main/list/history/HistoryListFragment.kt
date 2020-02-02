package org.koitharu.kotatsu.ui.main.list.history

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import kotlinx.android.synthetic.main.fragment_list.*
import moxy.ktx.moxyPresenter
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.MangaHistory
import org.koitharu.kotatsu.ui.main.list.MangaListFragment
import org.koitharu.kotatsu.ui.main.list.MangaListView

class HistoryListFragment : MangaListFragment<MangaHistory>(), MangaListView<MangaHistory>{

	private val presenter by moxyPresenter(factory = ::HistoryListPresenter)

	override fun onRequestMoreItems(offset: Int) {
		presenter.loadList(offset)
	}

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
		inflater.inflate(R.menu.opt_history, menu)
		super.onCreateOptionsMenu(menu, inflater)
	}

	override fun onOptionsItemSelected(item: MenuItem) = when(item.itemId) {
		R.id.action_clear_history -> {
			presenter.clearHistory()
			true
		}
		else -> super.onOptionsItemSelected(item)
	}

	override fun getTitle(): CharSequence? {
		return getString(R.string.history)
	}

	override fun setUpEmptyListHolder() {
		textView_holder.setText(R.string.history_is_empty)
		textView_holder.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
	}

	companion object {

		fun newInstance() = HistoryListFragment()
	}
}