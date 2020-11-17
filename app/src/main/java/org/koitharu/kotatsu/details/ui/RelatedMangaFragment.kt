package org.koitharu.kotatsu.details.ui

import android.os.Bundle
import android.view.View
import org.koin.android.viewmodel.ext.android.sharedViewModel
import org.koitharu.kotatsu.list.ui.MangaListFragment

class RelatedMangaFragment : MangaListFragment() {

	override val viewModel by sharedViewModel<DetailsViewModel>()

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		isSwipeRefreshEnabled = false
	}

	override fun onRequestMoreItems(offset: Int) {
		if (offset == 0) {
			viewModel.loadRelated()
		}
	}
}