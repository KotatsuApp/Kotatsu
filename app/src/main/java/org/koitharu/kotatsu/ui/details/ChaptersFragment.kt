package org.koitharu.kotatsu.ui.details

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
import moxy.ktx.moxyPresenter
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.*
import org.koitharu.kotatsu.ui.common.BaseFragment
import org.koitharu.kotatsu.ui.common.list.OnRecyclerItemClickListener
import org.koitharu.kotatsu.ui.download.DownloadService
import org.koitharu.kotatsu.ui.reader.ReaderActivity
import org.koitharu.kotatsu.utils.ext.resolveDp

class ChaptersFragment : BaseFragment(R.layout.fragment_chapters), MangaDetailsView,
	OnRecyclerItemClickListener<MangaChapter>, ActionMode.Callback {

	@Suppress("unused")
	private val presenter by moxyPresenter(factory = MangaDetailsPresenter.Companion::getInstance)

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
	}

	override fun onMangaUpdated(manga: Manga) {
		this.manga = manga
		adapter.replaceData(manga.chapters.orEmpty())
		scrollToCurrent()
	}

	override fun onLoadingStateChanged(isLoading: Boolean) {
		progressBar.isVisible = isLoading
	}

	override fun onError(e: Throwable) = Unit //handled in activity

	override fun onMangaRemoved(manga: Manga) = Unit //handled in activity

	override fun onHistoryChanged(history: MangaHistory?) {
		adapter.currentChapterId = history?.chapterId
		scrollToCurrent()
	}

	override fun onNewChaptersChanged(newChapters: Int) {
		adapter.newChaptersCount = newChapters
	}

	override fun onFavouriteChanged(categories: List<FavouriteCategory>) = Unit

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