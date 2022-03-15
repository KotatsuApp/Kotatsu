package org.koitharu.kotatsu.remotelist.ui

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.list.ui.MangaListFragment
import org.koitharu.kotatsu.list.ui.filter.FilterBottomSheet
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.reader.ui.SimpleSettingsActivity
import org.koitharu.kotatsu.utils.ext.serializableArgument
import org.koitharu.kotatsu.utils.ext.withArgs

class RemoteListFragment : MangaListFragment() {

	override val viewModel by viewModel<RemoteListViewModel> {
		parametersOf(source)
	}

	private val source by serializableArgument<MangaSource>(ARG_SOURCE)

	override fun onScrolledToEnd() {
		viewModel.loadNextPage()
	}

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
		super.onCreateOptionsMenu(menu, inflater)
		inflater.inflate(R.menu.opt_list_remote, menu)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.action_source_settings -> {
				startActivity(
					SimpleSettingsActivity.newSourceSettingsIntent(
						context ?: return false,
						source,
					)
				)
				true
			}
			R.id.action_filter -> {
				onFilterClick()
				true
			}
			else -> super.onOptionsItemSelected(item)
		}
	}

	override fun onFilterClick() {
		FilterBottomSheet.show(childFragmentManager)
	}

	override fun onEmptyActionClick() {
		viewModel.resetFilter()
	}

	companion object {

		private const val ARG_SOURCE = "provider"

		fun newInstance(provider: MangaSource) = RemoteListFragment().withArgs(1) {
			putSerializable(ARG_SOURCE, provider)
		}
	}
}