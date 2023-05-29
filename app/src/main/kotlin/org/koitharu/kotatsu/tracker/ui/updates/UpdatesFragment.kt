package org.koitharu.kotatsu.tracker.ui.updates

import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.list.ui.MangaListFragment

@AndroidEntryPoint
class UpdatesFragment : MangaListFragment() {

	override val viewModel by viewModels<UpdatesViewModel>()
	override val isSwipeRefreshEnabled = false

	override fun onScrolledToEnd() = Unit

	companion object {

		fun newInstance() = UpdatesFragment()
	}
}
