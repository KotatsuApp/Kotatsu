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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_chapters.*
import org.koin.android.viewmodel.ext.android.sharedViewModel
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseFragment
import org.koitharu.kotatsu.base.ui.list.OnRecyclerItemClickListener
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.model.MangaChapter
import org.koitharu.kotatsu.core.model.MangaHistory
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.download.DownloadService
import org.koitharu.kotatsu.reader.ui.ReaderActivity
import org.koitharu.kotatsu.utils.ext.resolveDp

class ChaptersFragment : BaseFragment(R.layout.fragment_chapters),
	OnRecyclerItemClickListener<MangaChapter>, ActionMode.Callback {

	private val viewModel by sharedViewModel<DetailsViewModel>()

	private var manga: Manga? = null

	private lateinit var adapter: ChaptersAdapter
	private var actionMode: ActionMode? = null

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		adapter = ChaptersAdapter(this)
		recyclerView_chapters.addItemDecoration(
			DividerItemDecoration(
				view.context,
				RecyclerView.VERTICAL
			)
		)
		recyclerView_chapters.setHasFixedSize(true)
		recyclerView_chapters.adapter = adapter

		viewModel.mangaData.observe(viewLifecycleOwner, this::onMangaUpdated)
		viewModel.isLoading.observe(viewLifecycleOwner, this::onLoadingStateChanged)
		viewModel.history.observe(viewLifecycleOwner, this::onHistoryChanged)
		viewModel.newChapters.observe(viewLifecycleOwner, this::onNewChaptersChanged)
	}

	private fun onMangaUpdated(manga: Manga) {
		this.manga = manga
		adapter.replaceData(manga.chapters.orEmpty())
		scrollToCurrent()
	}

	private fun onLoadingStateChanged(isLoading: Boolean) {
		progressBar.isVisible = isLoading
	}

	private fun onHistoryChanged(history: MangaHistory?) {
		adapter.currentChapterId = history?.chapterId
		scrollToCurrent()
	}

	private fun onNewChaptersChanged(newChapters: Int) {
		adapter.newChaptersCount = newChapters
	}

	override fun onItemClick(item: MangaChapter, position: Int, view: View) {
		if (adapter.checkedItemsCount != 0) {
			adapter.toggleItemChecked(item.id)
			if (adapter.checkedItemsCount == 0) {
				actionMode?.finish()
			} else {
				actionMode?.invalidate()
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
				manga ?: return,
				item.id
			), options.toBundle()
		)
	}

	override fun onItemLongClick(item: MangaChapter, position: Int, view: View): Boolean {
		if (actionMode == null) {
			actionMode = (activity as? AppCompatActivity)?.startSupportActionMode(this)
		}
		return actionMode?.also {
			adapter.setItemIsChecked(item.id, true)
			it.invalidate()
		} != null
	}

	private fun scrollToCurrent() {
		val pos = (recyclerView_chapters.adapter as? ChaptersAdapter)?.currentChapterPosition
			?: RecyclerView.NO_POSITION
		if (pos != RecyclerView.NO_POSITION) {
			(recyclerView_chapters.layoutManager as? LinearLayoutManager)
				?.scrollToPositionWithOffset(pos, resources.resolveDp(40))
		}
	}

	override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.action_save -> {
				DownloadService.start(
					context ?: return false,
					manga ?: return false,
					adapter.checkedItemsIds
				)
				mode.finish()
				true
			}
			R.id.action_select_all -> {
				adapter.checkAll()
				mode.invalidate()
				true
			}
			else -> false
		}
	}

	override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
		mode.menuInflater.inflate(R.menu.mode_chapters, menu)
		menu.findItem(R.id.action_save).isVisible = manga?.source != MangaSource.LOCAL
		mode.title = manga?.title
		return true
	}

	override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
		val count = adapter.checkedItemsCount
		mode.subtitle = resources.getQuantityString(
			R.plurals.chapters_from_x,
			count,
			count,
			adapter.itemCount
		)
		return true
	}

	override fun onDestroyActionMode(mode: ActionMode?) {
		adapter.clearChecked()
		actionMode = null
	}
}