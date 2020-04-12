package org.koitharu.kotatsu.ui.main.list.favourites

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import kotlinx.android.synthetic.main.fragment_list.*
import moxy.ktx.moxyPresenter
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.ui.main.list.MangaListFragment
import org.koitharu.kotatsu.ui.main.list.MangaListView
import org.koitharu.kotatsu.ui.main.list.favourites.categories.CategoriesActivity

class FavouritesListFragment : MangaListFragment<Unit>(), MangaListView<Unit> {

	private val presenter by moxyPresenter(factory = ::FavouritesListPresenter)

	override fun onRequestMoreItems(offset: Int) {
		presenter.loadList(offset)
	}

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
		inflater.inflate(R.menu.opt_favourites, menu)
		super.onCreateOptionsMenu(menu, inflater)
	}

	override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
		R.id.action_categories -> {
			context?.let {
				startActivity(CategoriesActivity.newIntent(it))
			}
			true
		}
		else -> super.onOptionsItemSelected(item)
	}

	override fun getTitle(): CharSequence? {
		return getString(R.string.favourites)
	}

	override fun setUpEmptyListHolder() {
		textView_holder.setText(R.string.you_have_not_favourites_yet)
		textView_holder.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
	}

	companion object {

		fun newInstance() = FavouritesListFragment()
	}
}