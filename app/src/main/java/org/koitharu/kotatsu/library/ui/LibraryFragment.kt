package org.koitharu.kotatsu.library.ui

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.graphics.Insets
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import org.koin.android.ext.android.get
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseFragment
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.databinding.FragmentLibraryBinding
import org.koitharu.kotatsu.details.ui.DetailsActivity
import org.koitharu.kotatsu.download.ui.service.DownloadService
import org.koitharu.kotatsu.favourites.ui.categories.select.FavouriteCategoriesBottomSheet
import org.koitharu.kotatsu.history.ui.HistoryListFragment
import org.koitharu.kotatsu.library.ui.adapter.LibraryAdapter
import org.koitharu.kotatsu.library.ui.model.LibraryGroupModel
import org.koitharu.kotatsu.list.ui.ItemSizeResolver
import org.koitharu.kotatsu.list.ui.MangaSelectionDecoration
import org.koitharu.kotatsu.list.ui.adapter.MangaListListener
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.utils.ShareHelper
import org.koitharu.kotatsu.utils.ext.findViewsByType
import org.koitharu.kotatsu.utils.ext.getDisplayMessage

class LibraryFragment : BaseFragment<FragmentLibraryBinding>(), MangaListListener, ActionMode.Callback {

	private val viewModel by viewModel<LibraryViewModel>()
	private var adapter: LibraryAdapter? = null
	private var selectionDecoration: MangaSelectionDecoration? = null
	private var actionMode: ActionMode? = null

	override fun onInflateView(inflater: LayoutInflater, container: ViewGroup?): FragmentLibraryBinding {
		return FragmentLibraryBinding.inflate(inflater, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		val sizeResolver = ItemSizeResolver(resources, get())
		val itemCLickListener = object : OnListItemClickListener<LibraryGroupModel> {
			override fun onItemClick(item: LibraryGroupModel, view: View) {

			}
		}
		selectionDecoration = MangaSelectionDecoration(view.context)
		adapter = LibraryAdapter(
			lifecycleOwner = viewLifecycleOwner,
			coil = get(),
			listener = this,
			itemClickListener = itemCLickListener,
			sizeResolver = sizeResolver,
			selectionDecoration = checkNotNull(selectionDecoration),
		)
		binding.recyclerView.adapter = adapter
		binding.recyclerView.setHasFixedSize(true)

		viewModel.content.observe(viewLifecycleOwner, ::onListChanged)
		viewModel.onError.observe(viewLifecycleOwner, ::onError)
	}

	override fun onDestroyView() {
		super.onDestroyView()
		adapter = null
		selectionDecoration = null
		actionMode = null
	}

	override fun onItemClick(item: Manga, view: View) {
		if (selectionDecoration?.checkedItemsCount != 0) {
			selectionDecoration?.toggleItemChecked(item.id)
			if (selectionDecoration?.checkedItemsCount == 0) {
				actionMode?.finish()
			} else {
				actionMode?.invalidate()
				invalidateItemDecorations()
			}
			return
		}
		val intent = DetailsActivity.newIntent(view.context, item)
		startActivity(intent)
	}

	override fun onItemLongClick(item: Manga, view: View): Boolean {
		if (actionMode == null) {
			actionMode = (activity as? AppCompatActivity)?.startSupportActionMode(this)
		}
		return actionMode?.also {
			selectionDecoration?.setItemIsChecked(item.id, true)
			invalidateItemDecorations()
			it.invalidate()
		} != null
	}

	override fun onRetryClick(error: Throwable) = Unit

	override fun onTagRemoveClick(tag: MangaTag) = Unit

	override fun onFilterClick() = Unit

	override fun onEmptyActionClick() = Unit

	override fun onWindowInsetsChanged(insets: Insets) {
		binding.recyclerView.updatePadding(
			left = insets.left,
			right = insets.right,
			top = insets.top,
			bottom = insets.bottom,
		)
	}

	override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
		mode.menuInflater.inflate(R.menu.mode_remote, menu)
		return true
	}

	override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
		mode.title = selectionDecoration?.checkedItemsCount?.toString()
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

	override fun onDestroyActionMode(mode: ActionMode) {
		selectionDecoration?.clearSelection()
		invalidateItemDecorations()
		actionMode = null
	}

	private fun collectSelectedItems(): Set<Manga> {
		val ids = selectionDecoration?.checkedItemsIds
		if (ids.isNullOrEmpty()) {
			return emptySet()
		}
		return emptySet()//viewModel.getItems(ids)
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

	companion object {

		fun newInstance() = LibraryFragment()
	}
}