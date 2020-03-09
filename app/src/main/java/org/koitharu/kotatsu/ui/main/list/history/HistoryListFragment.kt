package org.koitharu.kotatsu.ui.main.list.history

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_list.*
import moxy.ktx.moxyPresenter
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.model.MangaHistory
import org.koitharu.kotatsu.ui.main.list.MangaListFragment
import org.koitharu.kotatsu.ui.main.list.MangaListView
import org.koitharu.kotatsu.utils.ext.ellipsize

class HistoryListFragment : MangaListFragment<MangaHistory>(), MangaListView<MangaHistory> {

	private val presenter by moxyPresenter(factory = ::HistoryListPresenter)

	override fun onRequestMoreItems(offset: Int) {
		presenter.loadList(offset)
	}

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
		inflater.inflate(R.menu.opt_history, menu)
		super.onCreateOptionsMenu(menu, inflater)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.action_clear_history -> {
				AlertDialog.Builder(context ?: return false)
					.setTitle(R.string.clear_history)
					.setMessage(R.string.text_clear_history_prompt)
					.setNegativeButton(android.R.string.cancel, null)
					.setPositiveButton(R.string.clear) { _, _ ->
						presenter.clearHistory()
					}.show()
				true
			}
			else -> super.onOptionsItemSelected(item)
		}
	}

	override fun getTitle(): CharSequence? {
		return getString(R.string.history)
	}

	override fun setUpEmptyListHolder() {
		textView_holder.setText(R.string.history_is_empty)
		textView_holder.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
	}

	override fun onCreatePopupMenu(inflater: MenuInflater, menu: Menu, data: Manga) {
		super.onCreatePopupMenu(inflater, menu, data)
		inflater.inflate(R.menu.popup_history, menu)
	}

	override fun onPopupMenuItemSelected(item: MenuItem, data: Manga): Boolean {
		return when (item.itemId) {
			R.id.action_remove -> {
				presenter.removeFromHistory(data)
				true
			}
			else -> super.onPopupMenuItemSelected(item, data)
		}
	}

	override fun onItemRemoved(item: Manga) {
		super.onItemRemoved(item)
		Snackbar.make(
			recyclerView, getString(
				R.string._s_removed_from_history,
				item.title.ellipsize(16)
			), Snackbar.LENGTH_SHORT
		).show()
	}

	companion object {

		fun newInstance() = HistoryListFragment()
	}
}