package org.koitharu.kotatsu.ui.reader

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.activity_reader.*
import moxy.ktx.moxyPresenter
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.model.MangaHistory
import org.koitharu.kotatsu.core.model.MangaPage
import org.koitharu.kotatsu.ui.common.BaseActivity
import org.koitharu.kotatsu.utils.ext.showDialog

class ReaderActivity : BaseActivity(), ReaderView {

	private val presenter by moxyPresenter { ReaderPresenter() }

	private lateinit var state: ReaderState

	private lateinit var loader: PageLoader
	private lateinit var adapter: PagesAdapter

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_reader)
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		bottomBar.inflateMenu(R.menu.opt_reader_bottom)

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

		loader = PageLoader(this)
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
			ChaptersDialog.show(supportFragmentManager, state.manga.chapters.orEmpty())
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