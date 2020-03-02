package org.koitharu.kotatsu.ui.reader

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.commit
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_reader.*
import moxy.MvpDelegate
import moxy.ktx.moxyPresenter
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.model.MangaChapter
import org.koitharu.kotatsu.core.model.MangaHistory
import org.koitharu.kotatsu.core.model.MangaPage
import org.koitharu.kotatsu.core.prefs.ReaderMode
import org.koitharu.kotatsu.ui.common.BaseFullscreenActivity
import org.koitharu.kotatsu.ui.reader.standard.StandardReaderFragment
import org.koitharu.kotatsu.ui.reader.thumbnails.OnPageSelectListener
import org.koitharu.kotatsu.ui.reader.thumbnails.PagesThumbnailsSheet
import org.koitharu.kotatsu.ui.reader.wetoon.WebtoonReaderFragment
import org.koitharu.kotatsu.utils.GridTouchHelper
import org.koitharu.kotatsu.utils.ShareHelper
import org.koitharu.kotatsu.utils.anim.Motion
import org.koitharu.kotatsu.utils.ext.*

class ReaderActivity : BaseFullscreenActivity(), ReaderView, ChaptersDialog.OnChapterChangeListener,
	GridTouchHelper.OnGridTouchListener, OnPageSelectListener, ReaderConfigDialog.Callback,
	ReaderListener {

	private val presenter by moxyPresenter(factory = ReaderPresenter.Companion::getInstance)

	lateinit var state: ReaderState
		private set

	private lateinit var touchHelper: GridTouchHelper

	private val reader
		get() = supportFragmentManager.findFragmentById(R.id.container) as? BaseReaderFragment

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

		if (savedInstanceState?.containsKey(MvpDelegate.MOXY_DELEGATE_TAGS_KEY) != true) {
			presenter.loadChapter(state.manga, state.chapterId, ReaderAction.REPLACE)
		}
	}

	override fun onInitReader(mode: ReaderMode) {
		if (reader == null) {
			setReader(mode)
		}
	}

	override fun onPagesLoaded(chapterId: Long, pages: List<MangaPage>, action: ReaderAction) = Unit

	override fun onPause() {
		reader?.let {
			state = state.copy(page = it.findCurrentPageIndex(state.chapterId))
			presenter.saveState(state)
		}
		super.onPause()
	}

	override fun onCreateOptionsMenu(menu: Menu?): Boolean {
		menuInflater.inflate(R.menu.opt_reader_top, menu)
		return super.onCreateOptionsMenu(menu)
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		outState.putParcelable(EXTRA_STATE, state)
	}

	override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
		R.id.action_settings -> {
			ReaderConfigDialog.show(
				supportFragmentManager, when (reader) {
					is StandardReaderFragment -> ReaderMode.STANDARD
					is WebtoonReaderFragment -> ReaderMode.WEBTOON
					else -> ReaderMode.UNKNOWN
				}
			)
			true
		}
		R.id.action_chapters -> {
			ChaptersDialog.show(
				supportFragmentManager,
				state.manga.chapters.orEmpty(),
				state.chapterId
			)
			true
		}
		R.id.action_pages_thumbs -> {
			if (reader?.hasItems == true) {
				val pages = reader?.getPages(state.chapterId)
				if (pages != null) {
					PagesThumbnailsSheet.show(
						supportFragmentManager, pages,
						state.chapter?.name ?: title?.toString().orEmpty()
					)
				} else {
					showWaitWhileLoading()
				}
			} else {
				showWaitWhileLoading()
			}
			true
		}
		R.id.action_save_page -> {
			if (reader?.hasItems == true) {
				requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) {
					if (it) {
						presenter.savePage(
							resolver = contentResolver,
							page = reader?.currentPage ?: return@requestPermission
						)
					}
				}
			} else {
				showWaitWhileLoading()
			}
			true
		}
		else -> super.onOptionsItemSelected(item)
	}

	override fun onLoadingStateChanged(isLoading: Boolean) {
		val hasPages = reader?.hasItems == true
		layout_loading.isVisible = isLoading && !hasPages
		progressBar_bottom.isVisible = isLoading && hasPages
	}

	override fun onError(e: Exception) {
		showDialog {
			setTitle(R.string.error_occurred)
			setMessage(e.message)
			setPositiveButton(R.string.close, null)
			if (reader?.hasItems != true) {
				setOnDismissListener {
					finish()
				}
			}
		}
	}

	override fun onGridTouch(area: Int) {
		when (area) {
			GridTouchHelper.AREA_CENTER -> {
				setUiIsVisible(!appbar_top.isVisible)
			}
			GridTouchHelper.AREA_TOP,
			GridTouchHelper.AREA_LEFT -> {
				reader?.switchPageBy(-1)
			}
			GridTouchHelper.AREA_BOTTOM,
			GridTouchHelper.AREA_RIGHT -> {
				reader?.switchPageBy(1)
			}
		}
	}

	override fun onProcessTouch(rawX: Int, rawY: Int): Boolean {
		return if (appbar_top.hasGlobalPoint(rawX, rawY)
			|| appbar_bottom.hasGlobalPoint(rawX, rawY)
		) {
			false
		} else {
			val targets = rootLayout.hitTest(rawX, rawY)
			targets.none { it.hasOnClickListeners() }
		}
	}

	override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
		touchHelper.dispatchTouchEvent(ev)
		return super.dispatchTouchEvent(ev)
	}

	override fun onChapterChanged(chapter: MangaChapter) {
		state = state.copy(
			chapterId = chapter.id,
			page = 0
		)
		presenter.loadChapter(state.manga, chapter.id, ReaderAction.REPLACE)
	}

	override fun onPageSelected(page: MangaPage) {
		reader?.let {
			val index = it.pages.indexOfFirst { x -> x.id == page.id }
			if (index != -1) {
				it.setCurrentPage(index, false)
			}
		}
	}

	override fun onReaderModeChanged(mode: ReaderMode) {
		reader?.let {
			state = state.copy(page = it.findCurrentPageIndex(state.chapterId))
		}
		presenter.saveState(state, mode)
		setReader(mode)
		setUiIsVisible(false)
	}

	override fun onChaptersLoader(chapters: List<MangaChapter>) {
		state = state.copy(manga = state.manga.copy(chapters = chapters))
	}

	override fun onPageSaved(uri: Uri?) {
		if (uri != null) {
			Snackbar.make(container, R.string.page_saved, Snackbar.LENGTH_LONG)
				.setAction(R.string.share) {
					ShareHelper.shareImage(this, uri)
				}.show()
		} else {
			Snackbar.make(container, R.string.error_occurred, Snackbar.LENGTH_SHORT).show()
		}
	}

	override fun onPageChanged(chapter: MangaChapter, page: Int, total: Int) {
		if (chapter.id != state.chapterId) {
			title = chapter.name
			state = state.copy(chapterId = chapter.id)
			presenter.saveState(state)
			state.manga.chapters?.run {
				supportActionBar?.subtitle =
					getString(R.string.chapter_d_of_d, chapter.number, size)
			}
		}
	}

	private fun showWaitWhileLoading() {
		Toast.makeText(this, R.string.wait_for_loading_finish, Toast.LENGTH_SHORT).apply {
			setGravity(Gravity.CENTER, 0, 0)
		}.show()
	}

	private fun setUiIsVisible(isUiVisible: Boolean) {
		if (appbar_top.isVisible != isUiVisible) {
			if (isUiVisible) {
				appbar_top.showAnimated(Motion.SlideTop)
				appbar_bottom.showAnimated(Motion.SlideBottom)
				showSystemUI()
			} else {
				appbar_top.hideAnimated(Motion.SlideTop)
				appbar_bottom.hideAnimated(Motion.SlideBottom)
				hideSystemUI()
			}
		}
	}

	private fun setReader(mode: ReaderMode) {
		val currentReader = reader
		when (mode) {
			ReaderMode.WEBTOON -> if (currentReader !is WebtoonReaderFragment) {
				supportFragmentManager.commit {
					replace(R.id.container, WebtoonReaderFragment())
				}
			}
			else -> if (currentReader !is StandardReaderFragment) {
				supportFragmentManager.commit {
					replace(R.id.container, StandardReaderFragment())
				}
			}
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