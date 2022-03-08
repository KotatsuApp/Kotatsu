package org.koitharu.kotatsu.remotelist.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.FragmentResultListener
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.list.ui.MangaListFragment
import org.koitharu.kotatsu.list.ui.filter.FilterBottomSheet
import org.koitharu.kotatsu.list.ui.filter.FilterState
import org.koitharu.kotatsu.reader.ui.SimpleSettingsActivity
import org.koitharu.kotatsu.utils.ext.parcelableArgument
import org.koitharu.kotatsu.utils.ext.withArgs

class RemoteListFragment : MangaListFragment(), FragmentResultListener {

	override val viewModel by viewModel<RemoteListViewModel> {
		parametersOf(source)
	}

	private val source by parcelableArgument<MangaSource>(ARG_SOURCE)

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		childFragmentManager.setFragmentResultListener(FilterBottomSheet.REQUEST_KEY, viewLifecycleOwner, this)
	}

	override fun onScrolledToEnd() {
		viewModel.loadNextPage()
	}

	override fun getTitle(): CharSequence {
		return source.title
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
		FilterBottomSheet.show(childFragmentManager, source, viewModel.filter)
	}

	override fun onEmptyActionClick() {
		viewModel.applyFilter(FilterState(viewModel.filter.sortOrder, emptySet()))
	}

	override fun onFragmentResult(requestKey: String, result: Bundle) {
		when (requestKey) {
			FilterBottomSheet.REQUEST_KEY -> viewModel.applyFilter(
				result.getParcelable(FilterBottomSheet.ARG_STATE) ?: return
			)
		}
	}

	companion object {

		private const val ARG_SOURCE = "provider"

		fun newInstance(provider: MangaSource) = RemoteListFragment().withArgs(1) {
			putParcelable(ARG_SOURCE, provider)
		}
	}
}