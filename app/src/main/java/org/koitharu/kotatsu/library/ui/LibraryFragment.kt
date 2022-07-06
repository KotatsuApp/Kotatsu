package org.koitharu.kotatsu.library.ui

import android.os.Bundle
import android.view.*
import androidx.appcompat.view.ActionMode
import androidx.core.graphics.Insets
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import org.koin.android.ext.android.get
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.domain.reverseAsync
import org.koitharu.kotatsu.base.ui.BaseFragment
import org.koitharu.kotatsu.base.ui.list.SectionedSelectionController
import org.koitharu.kotatsu.base.ui.list.decor.AbstractSelectionItemDecoration
import org.koitharu.kotatsu.base.ui.util.ReversibleAction
import org.koitharu.kotatsu.databinding.FragmentLibraryBinding
import org.koitharu.kotatsu.details.ui.DetailsActivity
import org.koitharu.kotatsu.download.ui.service.DownloadService
import org.koitharu.kotatsu.favourites.ui.FavouritesActivity
import org.koitharu.kotatsu.favourites.ui.categories.select.FavouriteCategoriesBottomSheet
import org.koitharu.kotatsu.history.ui.HistoryActivity
import org.koitharu.kotatsu.library.ui.adapter.LibraryAdapter
import org.koitharu.kotatsu.library.ui.adapter.LibraryListEventListener
import org.koitharu.kotatsu.library.ui.model.LibrarySectionModel
import org.koitharu.kotatsu.list.ui.ItemSizeResolver
import org.koitharu.kotatsu.list.ui.MangaSelectionDecoration
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.util.flattenTo
import org.koitharu.kotatsu.utils.ShareHelper
import org.koitharu.kotatsu.utils.ext.addMenuProvider
import org.koitharu.kotatsu.utils.ext.findViewsByType
import org.koitharu.kotatsu.utils.ext.getDisplayMessage

class LibraryFragment : BaseFragment<FragmentLibraryBinding>(), LibraryListEventListener,
	SectionedSelectionController.Callback<LibrarySectionModel> {

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
			registryOwner = this,
			callback = this,
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
		addMenuProvider(LibraryMenuProvider(view.context, viewModel))

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

	override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
		mode.menuInflater.inflate(R.menu.mode_remote, menu)
		return true
	}

	override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
		mode.title = selectionController?.count?.toString()
		return true
	}

	override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
		val ctx = context ?: return false
		return when (item.itemId) {
			R.id.action_share -> {
				ShareHelper(ctx).shareMangaLinks(collectSelectedItems())
				mode.finish()
				true
			}
			R.id.action_favourite -> {
				FavouriteCategoriesBottomSheet.show(childFragmentManager, collectSelectedItems())
				mode.finish()
				true
			}
			R.id.action_save -> {
				DownloadService.confirmAndStart(ctx, collectSelectedItems())
				mode.finish()
				true
			}
			else -> false
		}
	}

	override fun onSelectionChanged(count: Int) {
		invalidateItemDecorations()
	}

	override fun onCreateItemDecoration(section: LibrarySectionModel): AbstractSelectionItemDecoration {
		return MangaSelectionDecoration(requireContext())
	}

	private fun collectSelectedItemsMap(): Map<LibrarySectionModel, Set<Manga>> {
		val snapshot = selectionController?.snapshot()
		if (snapshot.isNullOrEmpty()) {
			return emptyMap()
		}
		return snapshot.mapValues { (_, ids) -> viewModel.getManga(ids) }
	}

	private fun collectSelectedItems(): Set<Manga> {
		val snapshot = selectionController?.snapshot()
		if (snapshot.isNullOrEmpty()) {
			return emptySet()
		}
		return viewModel.getManga(snapshot.values.flattenTo(HashSet()))
	}

	private fun invalidateItemDecorations() {
		binding.recyclerView.findViewsByType(RecyclerView::class.java).forEach {
			it.invalidateItemDecorations()
		}
	}

	private fun onListChanged(list: List<ListModel>) {
		adapter?.items = list
	}

	private fun onError(e: Throwable) {
		Snackbar.make(
			binding.recyclerView,
			e.getDisplayMessage(resources),
			Snackbar.LENGTH_SHORT
		).show()
	}

	private fun onActionDone(action: ReversibleAction) {
		val handle = action.handle
		val length = if (handle == null) Snackbar.LENGTH_SHORT else Snackbar.LENGTH_LONG
		val snackbar = Snackbar.make(binding.recyclerView, action.stringResId, length)
		if (handle != null) {
			snackbar.setAction(R.string.undo) { handle.reverseAsync() }
		}
		snackbar.show()
	}

	companion object {

		fun newInstance() = LibraryFragment()
	}
}