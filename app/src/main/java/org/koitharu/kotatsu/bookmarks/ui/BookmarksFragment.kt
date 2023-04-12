package org.koitharu.kotatsu.bookmarks.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.view.ActionMode
import androidx.core.graphics.Insets
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import coil.ImageLoader
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.domain.reverseAsync
import org.koitharu.kotatsu.base.ui.BaseFragment
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.base.ui.list.SectionedSelectionController
import org.koitharu.kotatsu.base.ui.list.decor.AbstractSelectionItemDecoration
import org.koitharu.kotatsu.base.ui.list.decor.SpacingItemDecoration
import org.koitharu.kotatsu.base.ui.list.fastscroll.FastScroller
import org.koitharu.kotatsu.base.ui.util.ReversibleAction
import org.koitharu.kotatsu.bookmarks.data.ids
import org.koitharu.kotatsu.bookmarks.domain.Bookmark
import org.koitharu.kotatsu.bookmarks.ui.adapter.BookmarksGroupAdapter
import org.koitharu.kotatsu.bookmarks.ui.model.BookmarksGroup
import org.koitharu.kotatsu.core.exceptions.resolve.SnackbarErrorObserver
import org.koitharu.kotatsu.databinding.FragmentListSimpleBinding
import org.koitharu.kotatsu.details.ui.DetailsActivity
import org.koitharu.kotatsu.list.ui.adapter.ListStateHolderListener
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.main.ui.owners.AppBarOwner
import org.koitharu.kotatsu.main.ui.owners.SnackbarOwner
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.reader.ui.ReaderActivity
import org.koitharu.kotatsu.utils.ext.invalidateNestedItemDecorations
import org.koitharu.kotatsu.utils.ext.scaleUpActivityOptionsOf
import javax.inject.Inject

@AndroidEntryPoint
class BookmarksFragment :
	BaseFragment<FragmentListSimpleBinding>(),
	ListStateHolderListener,
	OnListItemClickListener<Bookmark>,
	SectionedSelectionController.Callback<Manga>,
	FastScroller.FastScrollListener {

	@Inject
	lateinit var coil: ImageLoader

	private val viewModel by viewModels<BookmarksViewModel>()
	private var adapter: BookmarksGroupAdapter? = null
	private var selectionController: SectionedSelectionController<Manga>? = null

	override fun onInflateView(inflater: LayoutInflater, container: ViewGroup?): FragmentListSimpleBinding {
		return FragmentListSimpleBinding.inflate(inflater, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		selectionController = SectionedSelectionController(
			activity = requireActivity(),
			owner = this,
			callback = this,
		)
		adapter = BookmarksGroupAdapter(
			lifecycleOwner = viewLifecycleOwner,
			coil = coil,
			listener = this,
			selectionController = checkNotNull(selectionController),
			bookmarkClickListener = this,
			groupClickListener = OnGroupClickListener(),
		)
		binding.recyclerView.adapter = adapter
		binding.recyclerView.setHasFixedSize(true)
		val spacingDecoration = SpacingItemDecoration(view.resources.getDimensionPixelOffset(R.dimen.grid_spacing))
		binding.recyclerView.addItemDecoration(spacingDecoration)

		viewModel.content.observe(viewLifecycleOwner, ::onListChanged)
		viewModel.onError.observe(viewLifecycleOwner, SnackbarErrorObserver(binding.recyclerView, this))
		viewModel.onActionDone.observe(viewLifecycleOwner, ::onActionDone)
	}

	override fun onDestroyView() {
		super.onDestroyView()
		adapter = null
		selectionController = null
	}

	override fun onItemClick(item: Bookmark, view: View) {
		if (selectionController?.onItemClick(item.manga, item.pageId) != true) {
			val intent = ReaderActivity.newIntent(view.context, item)
			startActivity(intent, scaleUpActivityOptionsOf(view).toBundle())
			Toast.makeText(view.context, R.string.incognito_mode, Toast.LENGTH_SHORT).show()
		}
	}

	override fun onItemLongClick(item: Bookmark, view: View): Boolean {
		return selectionController?.onItemLongClick(item.manga, item.pageId) ?: false
	}

	override fun onRetryClick(error: Throwable) = Unit

	override fun onEmptyActionClick() = Unit

	override fun onFastScrollStart(fastScroller: FastScroller) {
		(activity as? AppBarOwner)?.appBar?.setExpanded(false, true)
	}

	override fun onFastScrollStop(fastScroller: FastScroller) = Unit

	override fun onSelectionChanged(controller: SectionedSelectionController<Manga>, count: Int) {
		binding.recyclerView.invalidateNestedItemDecorations()
	}

	override fun onCreateActionMode(
		controller: SectionedSelectionController<Manga>,
		mode: ActionMode,
		menu: Menu,
	): Boolean {
		mode.menuInflater.inflate(R.menu.mode_bookmarks, menu)
		return true
	}

	override fun onActionItemClicked(
		controller: SectionedSelectionController<Manga>,
		mode: ActionMode,
		item: MenuItem,
	): Boolean {
		return when (item.itemId) {
			R.id.action_remove -> {
				val ids = selectionController?.snapshot() ?: return false
				viewModel.removeBookmarks(ids)
				mode.finish()
				true
			}

			else -> false
		}
	}

	override fun onCreateItemDecoration(
		controller: SectionedSelectionController<Manga>,
		section: Manga,
	): AbstractSelectionItemDecoration = BookmarksSelectionDecoration(requireContext())

	override fun onWindowInsetsChanged(insets: Insets) {
		binding.recyclerView.updatePadding(
			bottom = insets.bottom,
		)
		binding.recyclerView.fastScroller.updateLayoutParams<ViewGroup.MarginLayoutParams> {
			bottomMargin = insets.bottom
		}
	}

	private fun onListChanged(list: List<ListModel>) {
		adapter?.items = list
	}

	private fun onActionDone(action: ReversibleAction) {
		val handle = action.handle
		val length = if (handle == null) Snackbar.LENGTH_SHORT else Snackbar.LENGTH_LONG
		val snackbar = Snackbar.make((activity as SnackbarOwner).snackbarHost, action.stringResId, length)
		if (handle != null) {
			snackbar.setAction(R.string.undo) { handle.reverseAsync() }
		}
		snackbar.show()
	}

	private inner class OnGroupClickListener : OnListItemClickListener<BookmarksGroup> {

		override fun onItemClick(item: BookmarksGroup, view: View) {
			val controller = selectionController
			if (controller != null && controller.count > 0) {
				if (controller.getSectionCount(item.manga) == item.bookmarks.size) {
					controller.clearSelection(item.manga)
				} else {
					controller.addToSelection(item.manga, item.bookmarks.ids())
				}
				return
			}
			val intent = DetailsActivity.newIntent(view.context, item.manga)
			startActivity(intent)
		}

		override fun onItemLongClick(item: BookmarksGroup, view: View): Boolean {
			return selectionController?.addToSelection(item.manga, item.bookmarks.ids()) ?: false
		}
	}

	companion object {

		fun newInstance() = BookmarksFragment()
	}
}
