package org.koitharu.kotatsu.ui.details

import android.app.ActivityOptions
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_chapters.*
import moxy.ktx.moxyPresenter
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.model.MangaChapter
import org.koitharu.kotatsu.core.model.MangaHistory
import org.koitharu.kotatsu.ui.common.BaseFragment
import org.koitharu.kotatsu.ui.common.list.OnRecyclerItemClickListener
import org.koitharu.kotatsu.ui.download.DownloadService
import org.koitharu.kotatsu.ui.reader.ReaderActivity
import org.koitharu.kotatsu.utils.ext.showPopupMenu

class ChaptersFragment : BaseFragment(R.layout.fragment_chapters), MangaDetailsView,
	OnRecyclerItemClickListener<MangaChapter> {

	@Suppress("unused")
	private val presenter by moxyPresenter(factory = MangaDetailsPresenter.Companion::getInstance)

	private var manga: Manga? = null

	private lateinit var adapter: ChaptersAdapter

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
		view.showPopupMenu(R.menu.popup_chapter) {
			val ctx = context ?: return@showPopupMenu false
			val m = manga ?: return@showPopupMenu false
			when (it.itemId) {
				R.id.action_save_this -> DownloadService.start(ctx, m, setOf(item.id))
				R.id.action_save_this_next -> DownloadService.start(ctx, m, m.chapters.orEmpty()
					.filter { x -> x.number >= item.number }.map { x -> x.id })
				R.id.action_save_this_prev -> DownloadService.start(ctx, m, m.chapters.orEmpty()
					.filter { x -> x.number <= item.number }.map { x -> x.id })
				else -> return@showPopupMenu false
			}
			true
		}
		return true
	}

	private fun scrollToCurrent() {
		val pos = (recyclerView_chapters.adapter as? ChaptersAdapter)?.currentChapterPosition
			?: RecyclerView.NO_POSITION
		if (pos != RecyclerView.NO_POSITION) {
			(recyclerView_chapters.layoutManager as? LinearLayoutManager)
				?.scrollToPositionWithOffset(pos, 100)
		}
	}
}