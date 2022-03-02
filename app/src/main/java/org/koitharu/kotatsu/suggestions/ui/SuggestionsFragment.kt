package org.koitharu.kotatsu.suggestions.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import com.google.android.material.snackbar.Snackbar
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.list.ui.MangaListFragment
import org.koitharu.kotatsu.reader.ui.SimpleSettingsActivity

class SuggestionsFragment : MangaListFragment() {

	override val viewModel by viewModel<SuggestionsViewModel>()
	override val isSwipeRefreshEnabled = false

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setHasOptionsMenu(true)
	}

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
		super.onCreateOptionsMenu(menu, inflater)
		inflater.inflate(R.menu.opt_suggestions, menu)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.action_update -> {
				SuggestionsWorker.startNow(requireContext())
				Snackbar.make(
					binding.recyclerView,
					R.string.feed_will_update_soon,
					Snackbar.LENGTH_LONG,
				).show()
				true
			}
			R.id.action_settings -> {
				startActivity(SimpleSettingsActivity.newSuggestionsSettingsIntent(requireContext()))
				true
			}
			else -> super.onOptionsItemSelected(item)
		}
	}

	override fun onScrolledToEnd() = Unit

	override fun getTitle(): CharSequence? {
		return context?.getString(R.string.suggestions)
	}

	companion object {

		fun newInstance() = SuggestionsFragment()
	}
}