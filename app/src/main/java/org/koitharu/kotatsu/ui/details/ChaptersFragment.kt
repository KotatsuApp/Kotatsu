package org.koitharu.kotatsu.ui.details

import android.app.ActivityOptions
import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.PopupMenu
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
		if (item.source == MangaSource.LOCAL) {
			return false
		}
		return context?.run {
			val menu = PopupMenu(this, view)
			menu.inflate(R.menu.popup_chapter)
			menu.setOnMenuItemClickListener(PopupMenuListener(this, manga ?: return false, item))
			menu.show()
			true
		} ?: false
	}

	private fun scrollToCurrent() {
		val pos = (recyclerView_chapters.adapter as? ChaptersAdapter)?.currentChapterPosition
			?: RecyclerView.NO_POSITION
		if (pos != RecyclerView.NO_POSITION) {
			(recyclerView_chapters.layoutManager as? LinearLayoutManager)
				?.scrollToPositionWithOffset(pos, resources.resolveDp(40))
		}
	}

	private class PopupMenuListener(
		private val context: Context,
		private val manga: Manga,
		private val chapter: MangaChapter
	) : PopupMenu.OnMenuItemClickListener {

		override fun onMenuItemClick(item: MenuItem?): Boolean = when (item?.itemId) {
			R.id.action_save_this -> {
				DownloadService.start(context, manga, setOf(chapter.id))
				true
			}
			R.id.action_save_this_next -> {
				DownloadService.start(context, manga, manga.chapters.orEmpty()
					.filter { x -> x.number >= chapter.number }.map { x -> x.id })
				true
			}
			R.id.action_save_this_prev -> {
				DownloadService.start(context, manga, manga.chapters.orEmpty()
					.filter { x -> x.number <= chapter.number }.map { x -> x.id })
				true
			}
			else -> false
		}

	}
}