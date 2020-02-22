package org.koitharu.kotatsu.ui.reader

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.widget.Button
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import kotlinx.android.synthetic.main.activity_reader.*
import moxy.ktx.moxyPresenter
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.model.MangaChapter
import org.koitharu.kotatsu.core.model.MangaHistory
import org.koitharu.kotatsu.core.model.MangaPage
import org.koitharu.kotatsu.ui.common.BaseFullscreenActivity
import org.koitharu.kotatsu.ui.reader.thumbnails.OnPageSelectListener
import org.koitharu.kotatsu.ui.reader.thumbnails.PagesThumbnailsSheet
import org.koitharu.kotatsu.utils.GridTouchHelper
import org.koitharu.kotatsu.utils.anim.Motion
import org.koitharu.kotatsu.utils.ext.*

class ReaderActivity : BaseFullscreenActivity(), ReaderView, ChaptersDialog.OnChapterChangeListener,
	GridTouchHelper.OnGridTouchListener, OnPageSelectListener {

	private val presenter by moxyPresenter(factory = ::ReaderPresenter)

	private lateinit var state: ReaderState

	private lateinit var loader: PageLoader
	private lateinit var adapter: PagesAdapter
	private lateinit var touchHelper: GridTouchHelper

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_reader)
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		touchHelper = GridTouchHelper(this, this)
		toolbar_bottom.inflateMenu(R.menu.opt_reader_bottom)
		toolbar_bottom.setOnMenuItemClickListener(::onOptionsItemSelected)

		state = savedInstanceState?.getParcelable(EXTRA_STATE)
			?: intent.getParcelableExtra<ReaderState>(EXTRA_STATE)
					?: let {
				Toast.makeText(this, R.string.error_occurred, Toast.LENGTH_SHORT).show()
				finish()
				return
			}

		title = state.chapter?.name ?: state.manga.title
		state.manga.chapters?.run {
			supportActionBar?.subtitle =
				getString(R.string.chapter_d_of_d, state.chapter?.number ?: 0, size)
		}

		appbar_bottom.setOnApplyWindowInsetsListener { view, insets ->
			view.updatePadding(bottom = insets.systemWindowInsetBottom)
			insets
		}

		loader = PageLoader()
		adapter = PagesAdapter(loader)
		pager.adapter = adapter
		presenter.loadChapter(state)
	}

	override fun onDestroy() {
		loader.dispose()
		super.onDestroy()
	}

	override fun onPause() {
		state = state.copy(page = pager.currentItem)
		presenter.saveState(state)
		super.onPause()
	}

	override fun onCreateOptionsMenu(menu: Menu?): Boolean {
		menuInflater.inflate(R.menu.opt_reader_top, menu)
		return super.onCreateOptionsMenu(menu)
	}

	override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
		R.id.action_chapters -> {
			ChaptersDialog.show(
				supportFragmentManager,
				state.manga.chapters.orEmpty(),
				state.chapterId
			)
			true
		}
		R.id.action_pages_thumbs -> {
			PagesThumbnailsSheet.show(
				supportFragmentManager, adapter.items,
				state.chapter?.name ?: title?.toString().orEmpty()
			)
			true
		}
		else -> super.onOptionsItemSelected(item)
	}

	override fun onPagesReady(pages: List<MangaPage>, index: Int) {
		adapter.replaceData(pages)
		pager.setCurrentItem(index, false)
	}

	override fun onLoadingStateChanged(isLoading: Boolean) {
		layout_loading.isVisible = isLoading
	}

	override fun onError(e: Exception) {
		showDialog {
			setTitle(R.string.error_occurred)
			setMessage(e.message)
			setPositiveButton(R.string.close, null)
		}
	}

	override fun onGridTouch(area: Int) {
		when (area) {
			GridTouchHelper.AREA_CENTER -> {
				if (appbar_top.isVisible) {
					appbar_top.hideAnimated(Motion.SlideTop)
					appbar_bottom.hideAnimated(Motion.SlideBottom)
					hideSystemUI()
				} else {
					appbar_top.showAnimated(Motion.SlideTop)
					appbar_bottom.showAnimated(Motion.SlideBottom)
					showSystemUI()
				}
			}
			GridTouchHelper.AREA_TOP,
			GridTouchHelper.AREA_LEFT -> {
				pager.setCurrentItem(pager.currentItem - 1, true)
			}
			GridTouchHelper.AREA_BOTTOM,
			GridTouchHelper.AREA_RIGHT -> {
				pager.setCurrentItem(pager.currentItem + 1, true)
			}
		}
	}

	override fun onProcessTouch(rawX: Int, rawY: Int): Boolean {
		return if (appbar_top.hasGlobalPoint(rawX, rawY) || appbar_bottom.hasGlobalPoint(
				rawX,
				rawY
			)
		) {
			false
		} else {
			val target = rootLayout.hitTest(rawX, rawY)
			target !is Button
		}
	}

	override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
		touchHelper.dispatchTouchEvent(ev)
		return super.dispatchTouchEvent(ev)
	}

	override fun onChapterChanged(chapter: MangaChapter) {
		presenter.loadChapter(
			state.copy(
				chapterId = chapter.id,
				page = 0
			)
		)
	}

	override fun onPageSelected(page: MangaPage) {
		val index = adapter.items.indexOfFirst { x -> x.id == page.id }
		if (index != -1) {
			pager.setCurrentItem(index, false)
		}
	}

	companion object {

		private const val EXTRA_STATE = "state"

		fun newIntent(context: Context, state: ReaderState) =
			Intent(context, ReaderActivity::class.java)
				.putExtra(EXTRA_STATE, state)

		fun newIntent(context: Context, manga: Manga, chapterId: Long = -1) = newIntent(
			context, ReaderState(
				manga = manga,
				chapterId = if (chapterId == -1L) manga.chapters?.firstOrNull()?.id
					?: -1 else chapterId,
				page = 0
			)
		)

		fun newIntent(context: Context, manga: Manga, history: MangaHistory?) =
			if (history == null) {
				newIntent(context, manga)
			} else {
				newIntent(
					context, ReaderState(
						manga = manga,
						chapterId = history.chapterId,
						page = history.page
					)
				)
			}
	}
}