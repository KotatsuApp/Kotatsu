package org.koitharu.kotatsu.bookmarks.ui

import android.os.Bundle
import android.view.*
import androidx.appcompat.view.ActionMode
import androidx.core.graphics.Insets
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import coil.ImageLoader
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.domain.reverseAsync
import org.koitharu.kotatsu.base.ui.BaseFragment
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.base.ui.list.SectionedSelectionController
import org.koitharu.kotatsu.base.ui.list.decor.AbstractSelectionItemDecoration
import org.koitharu.kotatsu.base.ui.list.decor.SpacingItemDecoration
import org.koitharu.kotatsu.base.ui.util.ReversibleAction
import org.koitharu.kotatsu.bookmarks.data.ids
import org.koitharu.kotatsu.bookmarks.domain.Bookmark
import org.koitharu.kotatsu.bookmarks.ui.adapter.BookmarksGroupAdapter
import org.koitharu.kotatsu.bookmarks.ui.model.BookmarksGroup
import org.koitharu.kotatsu.databinding.FragmentListSimpleBinding
import org.koitharu.kotatsu.details.ui.DetailsActivity
import org.koitharu.kotatsu.list.ui.adapter.ListStateHolderListener
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.reader.ui.ReaderActivity
import org.koitharu.kotatsu.utils.ext.getDisplayMessage
import org.koitharu.kotatsu.utils.ext.invalidateNestedItemDecorations
import org.koitharu.kotatsu.utils.ext.scaleUpActivityOptionsOf

@AndroidEntryPoint
class BookmarksFragment :
	BaseFragment<FragmentListSimpleBinding>(),
	ListStateHolderListener,
	OnListItemClickListener<Bookmark>,
	SectionedSelectionController.Callback<Manga> {

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
		viewModel.onError.observe(viewLifecycleOwner, ::onError)
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
		}
	}

	override fun onItemLongClick(item: Bookmark, view: View): Boolean {
		return selectionController?.onItemLongClick(item.manga, item.pageId) ?: false
	}

	override fun onRetryClick(error: Throwable) = Unit

	override fun onEmptyActionClick() = Unit

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
		Snackbar.make(
			binding.recyclerView,
			e.getDisplayMessage(resources),
			Snackbar.LENGTH_SHORT,
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
