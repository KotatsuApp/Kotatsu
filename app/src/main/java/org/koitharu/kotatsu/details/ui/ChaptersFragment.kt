package org.koitharu.kotatsu.details.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ActionMode
import androidx.core.graphics.Insets
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import com.google.android.material.snackbar.Snackbar
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseFragment
import org.koitharu.kotatsu.base.ui.list.ListSelectionController
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.databinding.FragmentChaptersBinding
import org.koitharu.kotatsu.details.ui.adapter.ChaptersAdapter
import org.koitharu.kotatsu.details.ui.adapter.ChaptersSelectionDecoration
import org.koitharu.kotatsu.details.ui.model.ChapterListItem
import org.koitharu.kotatsu.local.ui.LocalChaptersRemoveService
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.reader.ui.ReaderActivity
import org.koitharu.kotatsu.reader.ui.ReaderState
import org.koitharu.kotatsu.utils.RecyclerViewScrollCallback
import org.koitharu.kotatsu.utils.ext.scaleUpActivityOptionsOf
import kotlin.math.roundToInt

class ChaptersFragment :
	BaseFragment<FragmentChaptersBinding>(),
	OnListItemClickListener<ChapterListItem>,
	ListSelectionController.Callback2 {

	private val viewModel by activityViewModels<DetailsViewModel>()

	private var chaptersAdapter: ChaptersAdapter? = null
	private var selectionController: ListSelectionController? = null

	override fun onInflateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
	) = FragmentChaptersBinding.inflate(inflater, container, false)

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		chaptersAdapter = ChaptersAdapter(this)
		selectionController = ListSelectionController(
			activity = requireActivity(),
			decoration = ChaptersSelectionDecoration(view.context),
			registryOwner = this,
			callback = this,
		)
		with(binding.recyclerViewChapters) {
			checkNotNull(selectionController).attachToRecyclerView(this)
			setHasFixedSize(true)
			adapter = chaptersAdapter
		}
		viewModel.isLoading.observe(viewLifecycleOwner, this::onLoadingStateChanged)
		viewModel.chapters.observe(viewLifecycleOwner, this::onChaptersChanged)
		viewModel.isChaptersEmpty.observe(viewLifecycleOwner) {
			binding.textViewHolder.isVisible = it
		}
	}

	override fun onDestroyView() {
		chaptersAdapter = null
		selectionController = null
		super.onDestroyView()
	}

	override fun onItemClick(item: ChapterListItem, view: View) {
		if (selectionController?.onItemClick(item.chapter.id) == true) {
			return
		}
		if (item.hasFlag(ChapterListItem.FLAG_MISSING)) {
			(activity as? DetailsActivity)?.showChapterMissingDialog(item.chapter.id)
			return
		}
		startActivity(
			ReaderActivity.newIntent(
				context = view.context,
				manga = viewModel.manga.value ?: return,
				state = ReaderState(item.chapter.id, 0, 0),
			),
			scaleUpActivityOptionsOf(view).toBundle(),
		)
	}

	override fun onItemLongClick(item: ChapterListItem, view: View): Boolean {
		return selectionController?.onItemLongClick(item.chapter.id) ?: false
	}

	override fun onActionItemClicked(controller: ListSelectionController, mode: ActionMode, item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.action_save -> {
				viewModel.download(selectionController?.snapshot())
				mode.finish()
				true
			}

			R.id.action_delete -> {
				val ids = selectionController?.peekCheckedIds()
				val manga = viewModel.manga.value
				when {
					ids.isNullOrEmpty() || manga == null -> Unit
					ids.size == manga.chapters?.size -> viewModel.deleteLocal()
					else -> {
						LocalChaptersRemoveService.start(requireContext(), manga, ids)
						Snackbar.make(
							binding.recyclerViewChapters,
							R.string.chapters_will_removed_background,
							Snackbar.LENGTH_LONG,
						).show()
					}
				}
				mode.finish()
				true
			}

			R.id.action_select_range -> {
				val items = chaptersAdapter?.items ?: return false
				val ids = HashSet(controller.peekCheckedIds())
				val buffer = HashSet<Long>()
				var isAdding = false
				for (x in items) {
					if (x.chapter.id in ids) {
						isAdding = true
						if (buffer.isNotEmpty()) {
							ids.addAll(buffer)
							buffer.clear()
						}
					} else if (isAdding) {
						buffer.add(x.chapter.id)
					}
				}
				controller.addAll(ids)
				true
			}

			R.id.action_select_all -> {
				val ids = chaptersAdapter?.items?.map { it.chapter.id } ?: return false
				controller.addAll(ids)
				true
			}

			R.id.action_mark_current -> {
				val id = controller.peekCheckedIds().singleOrNull() ?: return false
				viewModel.markChapterAsCurrent(id)
				mode.finish()
				true
			}

			else -> false
		}
	}

	override fun onCreateActionMode(controller: ListSelectionController, mode: ActionMode, menu: Menu): Boolean {
		mode.menuInflater.inflate(R.menu.mode_chapters, menu)
		return true
	}

	override fun onPrepareActionMode(controller: ListSelectionController, mode: ActionMode, menu: Menu): Boolean {
		val selectedIds = selectionController?.peekCheckedIds() ?: return false
		val allItems = chaptersAdapter?.items.orEmpty()
		val items = allItems.withIndex().filter { (_, x) -> x.chapter.id in selectedIds }
		menu.findItem(R.id.action_save).isVisible = items.none { (_, x) ->
			x.chapter.source == MangaSource.LOCAL
		}
		menu.findItem(R.id.action_delete).isVisible = items.all { (_, x) ->
			x.chapter.source == MangaSource.LOCAL
		}
		menu.findItem(R.id.action_select_all).isVisible = items.size < allItems.size
		menu.findItem(R.id.action_mark_current).isVisible = items.size == 1
		mode.title = items.size.toString()
		var hasGap = false
		for (i in 0 until items.size - 1) {
			if (items[i].index + 1 != items[i + 1].index) {
				hasGap = true
				break
			}
		}
		menu.findItem(R.id.action_select_range).isVisible = hasGap
		return true
	}

	override fun onSelectionChanged(controller: ListSelectionController, count: Int) {
		binding.recyclerViewChapters.invalidateItemDecorations()
	}

	override fun onWindowInsetsChanged(insets: Insets) = Unit

	private fun onChaptersChanged(list: List<ChapterListItem>) {
		val adapter = chaptersAdapter ?: return
		if (adapter.itemCount == 0) {
			val position = list.indexOfFirst { it.hasFlag(ChapterListItem.FLAG_CURRENT) } - 1
			if (position > 0) {
				val offset = (resources.getDimensionPixelSize(R.dimen.chapter_list_item_height) * 0.6).roundToInt()
				adapter.setItems(list, RecyclerViewScrollCallback(binding.recyclerViewChapters, position, offset))
			} else {
				adapter.items = list
			}
		} else {
			adapter.items = list
		}
	}

	private fun onLoadingStateChanged(isLoading: Boolean) {
		binding.progressBar.isVisible = isLoading
	}
}
