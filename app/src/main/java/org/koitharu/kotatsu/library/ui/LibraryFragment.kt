package org.koitharu.kotatsu.library.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.Insets
import androidx.core.view.updatePadding
import com.google.android.material.snackbar.Snackbar
import org.koin.android.ext.android.get
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.domain.reverseAsync
import org.koitharu.kotatsu.base.ui.BaseFragment
import org.koitharu.kotatsu.base.ui.list.SectionedSelectionController
import org.koitharu.kotatsu.base.ui.util.ReversibleAction
import org.koitharu.kotatsu.databinding.FragmentLibraryBinding
import org.koitharu.kotatsu.details.ui.DetailsActivity
import org.koitharu.kotatsu.favourites.ui.FavouritesActivity
import org.koitharu.kotatsu.history.ui.HistoryActivity
import org.koitharu.kotatsu.library.ui.adapter.LibraryAdapter
import org.koitharu.kotatsu.library.ui.adapter.LibraryListEventListener
import org.koitharu.kotatsu.library.ui.model.LibrarySectionModel
import org.koitharu.kotatsu.list.ui.ItemSizeResolver
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.main.ui.BottomNavOwner
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.utils.ext.addMenuProvider
import org.koitharu.kotatsu.utils.ext.getDisplayMessage

class LibraryFragment :
	BaseFragment<FragmentLibraryBinding>(),
	LibraryListEventListener {

	private val viewModel by viewModel<LibraryViewModel>()
	private var adapter: LibraryAdapter? = null
	private var selectionController: SectionedSelectionController<LibrarySectionModel>? = null

	override fun onInflateView(inflater: LayoutInflater, container: ViewGroup?): FragmentLibraryBinding {
		return FragmentLibraryBinding.inflate(inflater, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		val sizeResolver = ItemSizeResolver(resources, get())
		selectionController = SectionedSelectionController(
			activity = requireActivity(),
			owner = this,
			callback = LibrarySelectionCallback(binding.recyclerView, childFragmentManager, viewModel),
		)
		adapter = LibraryAdapter(
			lifecycleOwner = viewLifecycleOwner,
			coil = get(),
			listener = this,
			sizeResolver = sizeResolver,
			selectionController = checkNotNull(selectionController),
		)
		binding.recyclerView.adapter = adapter
		binding.recyclerView.setHasFixedSize(true)
		addMenuProvider(LibraryMenuProvider(view.context, childFragmentManager, viewModel))

		viewModel.content.observe(viewLifecycleOwner, ::onListChanged)
		viewModel.onError.observe(viewLifecycleOwner, ::onError)
		viewModel.onActionDone.observe(viewLifecycleOwner, ::onActionDone)
	}

	override fun onDestroyView() {
		super.onDestroyView()
		adapter = null
		selectionController = null
	}

	override fun onItemClick(item: Manga, section: LibrarySectionModel, view: View) {
		if (selectionController?.onItemClick(section, item.id) != true) {
			val intent = DetailsActivity.newIntent(view.context, item)
			startActivity(intent)
		}
	}

	override fun onItemLongClick(item: Manga, section: LibrarySectionModel, view: View): Boolean {
		return selectionController?.onItemLongClick(section, item.id) ?: false
	}

	override fun onSectionClick(section: LibrarySectionModel, view: View) {
		selectionController?.clear()
		val intent = when (section) {
			is LibrarySectionModel.History -> HistoryActivity.newIntent(view.context)
			is LibrarySectionModel.Favourites -> FavouritesActivity.newIntent(view.context, section.category)
		}
		startActivity(intent)
	}

	override fun onRetryClick(error: Throwable) = Unit

	override fun onEmptyActionClick() = Unit

	override fun onWindowInsetsChanged(insets: Insets) {
		binding.root.updatePadding(
			left = insets.left,
			right = insets.right,
		)
		binding.recyclerView.updatePadding(
			bottom = insets.bottom,
		)
	}

	private fun onListChanged(list: List<ListModel>) {
		adapter?.items = list
	}

	private fun onError(e: Throwable) {
		val snackbar = Snackbar.make(
			binding.recyclerView,
			e.getDisplayMessage(resources),
			Snackbar.LENGTH_SHORT,
		)
		snackbar.anchorView = (activity as? BottomNavOwner)?.bottomNav
		snackbar.show()
	}

	private fun onActionDone(action: ReversibleAction) {
		val handle = action.handle
		val length = if (handle == null) Snackbar.LENGTH_SHORT else Snackbar.LENGTH_LONG
		val snackbar = Snackbar.make(binding.recyclerView, action.stringResId, length)
		if (handle != null) {
			snackbar.setAction(R.string.undo) { handle.reverseAsync() }
		}
		snackbar.anchorView = (activity as? BottomNavOwner)?.bottomNav
		snackbar.show()
	}

	companion object {

		fun newInstance() = LibraryFragment()
	}
}
