package org.koitharu.kotatsu.details.ui

import android.app.ActivityOptions
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_chapters.*
import org.koin.android.viewmodel.ext.android.sharedViewModel
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseFragment
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.model.MangaChapter
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.details.ui.adapter.ChaptersAdapter
import org.koitharu.kotatsu.details.ui.adapter.ChaptersSelectionDecoration
import org.koitharu.kotatsu.details.ui.model.ChapterListItem
import org.koitharu.kotatsu.download.DownloadService
import org.koitharu.kotatsu.reader.ui.ReaderActivity

class ChaptersFragment : BaseFragment(R.layout.fragment_chapters),
	OnListItemClickListener<MangaChapter>, ActionMode.Callback {

	private val viewModel by sharedViewModel<DetailsViewModel>()

	private var chaptersAdapter: ChaptersAdapter? = null
	private var actionMode: ActionMode? = null
	private var selectionDecoration: ChaptersSelectionDecoration? = null

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		chaptersAdapter = ChaptersAdapter(this)
		selectionDecoration = ChaptersSelectionDecoration(view.context)
		with(recyclerView_chapters) {
			addItemDecoration(DividerItemDecoration(view.context, RecyclerView.VERTICAL))
			addItemDecoration(selectionDecoration!!)
			setHasFixedSize(true)
			adapter = chaptersAdapter
		}

		viewModel.isLoading.observe(viewLifecycleOwner, this::onLoadingStateChanged)
		viewModel.chapters.observe(viewLifecycleOwner, this::onChaptersChanged)
	}

	override fun onDestroyView() {
		chaptersAdapter = null
		selectionDecoration = null
		super.onDestroyView()
	}

	private fun onChaptersChanged(list: List<ChapterListItem>) {
		chaptersAdapter?.items = list
	}

	private fun onLoadingStateChanged(isLoading: Boolean) {
		progressBar.isVisible = isLoading
	}

	override fun onItemClick(item: MangaChapter, view: View) {
		if (selectionDecoration?.checkedItemsCount != 0) {
			selectionDecoration?.toggleItemChecked(item.id)
			if (selectionDecoration?.checkedItemsCount == 0) {
				actionMode?.finish()
			} else {
				actionMode?.invalidate()
				recyclerView_chapters.invalidateItemDecorations()
			}
			return
		}
		val options = ActivityOptions.makeScaleUpAnimation(
			view,
			0,
			0,
			view.measuredWidth,
			view.measuredHeight
		)
		startActivity(
			ReaderActivity.newIntent(
				context ?: return,
				viewModel.manga.value ?: return,
				item.id
			), options.toBundle()
		)
	}

	override fun onItemLongClick(item: MangaChapter, view: View): Boolean {
		if (actionMode == null) {
			actionMode = (activity as? AppCompatActivity)?.startSupportActionMode(this)
		}
		return actionMode?.also {
			selectionDecoration?.setItemIsChecked(item.id, true)
			recyclerView_chapters.invalidateItemDecorations()
			it.invalidate()
		} != null
	}

	override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.action_save -> {
				DownloadService.start(
					context ?: return false,
					viewModel.manga.value ?: return false,
					selectionDecoration?.checkedItemsIds
				)
				mode.finish()
				true
			}
			R.id.action_select_all -> {
				val ids = chaptersAdapter?.items?.map { it.chapter.id } ?: return false
				selectionDecoration?.checkAll(ids)
				recyclerView_chapters.invalidateItemDecorations()
				mode.invalidate()
				true
			}
			else -> false
		}
	}

	override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
		val manga = viewModel.manga.value
		mode.menuInflater.inflate(R.menu.mode_chapters, menu)
		menu.findItem(R.id.action_save).isVisible = manga?.source != MangaSource.LOCAL
		mode.title = manga?.title
		return true
	}

	override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
		val count = selectionDecoration?.checkedItemsCount ?: return false
		mode.subtitle = resources.getQuantityString(
			R.plurals.chapters_from_x,
			count,
			count,
			chaptersAdapter?.itemCount ?: 0
		)
		return true
	}

	override fun onDestroyActionMode(mode: ActionMode?) {
		selectionDecoration?.clearSelection()
		recyclerView_chapters.invalidateItemDecorations()
		actionMode = null
	}
}