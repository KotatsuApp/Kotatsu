package org.koitharu.kotatsu.suggestions.ui

import android.os.Bundle
import android.view.View
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.list.ui.MangaListFragment

class SuggestionsFragment : MangaListFragment() {

	override val viewModel by viewModel<SuggestionsViewModel>(mode = LazyThreadSafetyMode.NONE)
	override val isSwipeRefreshEnabled = false

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
	}

	override fun onScrolledToEnd() = Unit

	override fun getTitle(): CharSequence? {
		return context?.getString(R.string.suggestions)
	}

	companion object {

		fun newInstance() = SuggestionsFragment()
	}
}