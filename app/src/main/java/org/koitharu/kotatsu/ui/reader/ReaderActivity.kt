package org.koitharu.kotatsu.ui.reader

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.activity_reader.*
import moxy.ktx.moxyPresenter
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.model.MangaPage
import org.koitharu.kotatsu.ui.common.BaseActivity
import org.koitharu.kotatsu.utils.ext.showDialog

class ReaderActivity : BaseActivity(), ReaderView {

	private val presenter by moxyPresenter { ReaderPresenter() }

	private val manga by lazy(LazyThreadSafetyMode.NONE) {
		intent.getParcelableExtra<Manga>(EXTRA_MANGA)!!
	}
	private val chapterId by lazy(LazyThreadSafetyMode.NONE) {
		intent.getLongExtra(EXTRA_CHAPTER_ID, 0L)
	}

	private lateinit var loader: PageLoader
	private lateinit var adapter: PagesAdapter

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_reader)
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		bottomBar.inflateMenu(R.menu.opt_reader_bottom)

		val chapter = manga.chapters?.find { x -> x.id == chapterId }
		if (chapter == null) {
			// TODO
			finish()
			return
		}
		title = chapter.name
		manga.chapters?.run {
			supportActionBar?.subtitle = getString(R.string.chapter_d_of_d, chapter.number, size)
		}

		loader = PageLoader(this)
		adapter = PagesAdapter(loader)
		pager.adapter = adapter
		presenter.loadChapter(chapter)
	}

	override fun onDestroy() {
		loader.dispose()
		super.onDestroy()
	}

	override fun onPause() {
		presenter.addToHistory(manga, chapterId, pager.currentItem)
		super.onPause()
	}

	override fun onCreateOptionsMenu(menu: Menu?): Boolean {
		menuInflater.inflate(R.menu.opt_reader_top, menu)
		return super.onCreateOptionsMenu(menu)
	}

	override fun onOptionsItemSelected(item: MenuItem) = when(item.itemId) {
		R.id.action_chapters -> {
			ChaptersDialog.show(supportFragmentManager, manga.chapters.orEmpty())
			true
		}
		else -> super.onOptionsItemSelected(item)
	}

	override fun onPagesReady(pages: List<MangaPage>) {
		adapter.replaceData(pages)
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

		private const val EXTRA_MANGA = "manga"
		private const val EXTRA_CHAPTER_ID = "chapter_id"

		fun newIntent(context: Context, manga: Manga, chapterId: Long) = Intent(context, ReaderActivity::class.java)
			.putExtra(EXTRA_MANGA, manga)
			.putExtra(EXTRA_CHAPTER_ID, chapterId)
	}
}