package org.koitharu.kotatsu.ui.reader

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.postDelayed
import androidx.core.view.updatePadding
import androidx.fragment.app.commit
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_reader.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import moxy.MvpDelegate
import moxy.ktx.moxyPresenter
import org.koin.core.inject
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.model.MangaChapter
import org.koitharu.kotatsu.core.model.MangaHistory
import org.koitharu.kotatsu.core.model.MangaPage
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.ReaderMode
import org.koitharu.kotatsu.ui.common.BaseFullscreenActivity
import org.koitharu.kotatsu.ui.reader.base.AbstractReader
import org.koitharu.kotatsu.ui.reader.standard.PagerReaderFragment
import org.koitharu.kotatsu.ui.reader.thumbnails.OnPageSelectListener
import org.koitharu.kotatsu.ui.reader.thumbnails.PagesThumbnailsSheet
import org.koitharu.kotatsu.ui.reader.wetoon.WebtoonReaderFragment
import org.koitharu.kotatsu.utils.GridTouchHelper
import org.koitharu.kotatsu.utils.MangaShortcut
import org.koitharu.kotatsu.utils.ShareHelper
import org.koitharu.kotatsu.utils.anim.Motion
import org.koitharu.kotatsu.utils.ext.*

class ReaderActivity : BaseFullscreenActivity(), ReaderView, ChaptersDialog.OnChapterChangeListener,
	GridTouchHelper.OnGridTouchListener, OnPageSelectListener, ReaderConfigDialog.Callback,
	ReaderListener, SharedPreferences.OnSharedPreferenceChangeListener,
	View.OnApplyWindowInsetsListener, ActivityResultCallback<Boolean> {

	private val presenter by moxyPresenter(factory = ::ReaderPresenter)
	private val settings by inject<AppSettings>()

	lateinit var state: ReaderState
		private set

	private lateinit var touchHelper: GridTouchHelper
	private var isTapSwitchEnabled = true
	private var isVolumeKeysSwitchEnabled = false

	private val reader
		get() = supportFragmentManager.findFragmentById(R.id.container) as? AbstractReader

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_reader)
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		touchHelper = GridTouchHelper(this, this)
		toolbar_bottom.inflateMenu(R.menu.opt_reader_bottom)
		toolbar_bottom.setOnMenuItemClickListener(::onOptionsItemSelected)

		@Suppress("RemoveExplicitTypeArguments")
		state = savedInstanceState?.getParcelable<ReaderState>(EXTRA_STATE)
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

		rootLayout.setOnApplyWindowInsetsListener(this)

		settings.subscribe(this)
		loadSettings()

		if (savedInstanceState?.containsKey(MvpDelegate.MOXY_DELEGATE_TAGS_KEY) != true) {
			presenter.init(state.manga)
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
				GlobalScope.launch {
					safe {
						MangaShortcut(state.manga).addAppShortcut(applicationContext)
					}
				}
			}
		}
	}

	override fun onInitReader(manga: Manga, mode: ReaderMode) {
		val currentReader = reader
		when (mode) {
			ReaderMode.WEBTOON -> if (currentReader !is WebtoonReaderFragment) {
				supportFragmentManager.commit {
					replace(R.id.container, WebtoonReaderFragment.newInstance(state))
				}
			}
			else -> if (currentReader !is PagerReaderFragment) {
				supportFragmentManager.commit {
					replace(R.id.container, PagerReaderFragment.newInstance(state))
				}
			}
		}
		toolbar_bottom.menu.findItem(R.id.action_reader_mode).setIcon(
			when (mode) {
				ReaderMode.WEBTOON -> R.drawable.ic_script
				else -> R.drawable.ic_book_page
			}
		)
		appbar_top.postDelayed(1000) {
			setUiIsVisible(false)
		}
	}

	override fun onDestroy() {
		settings.unsubscribe(this)
		super.onDestroy()
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
		R.id.action_reader_mode -> {
			ReaderConfigDialog.show(
				supportFragmentManager, when (reader) {
					is PagerReaderFragment -> ReaderMode.STANDARD
					is WebtoonReaderFragment -> ReaderMode.WEBTOON
					else -> ReaderMode.UNKNOWN
				}
			)
			true
		}
		R.id.action_settings -> {
			startActivity(SimpleSettingsActivity.newReaderSettingsIntent(this))
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
				val pages = reader?.getPages()
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
				if (ContextCompat.checkSelfPermission(
						this,
						Manifest.permission.WRITE_EXTERNAL_STORAGE
					) == PackageManager.PERMISSION_GRANTED
				) {
					onActivityResult(true)
				} else {
					registerForActivityResult(
						ActivityResultContracts.RequestPermission(),
						this
					).launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
				}
			} else {
				showWaitWhileLoading()
			}
			true
		}
		else -> super.onOptionsItemSelected(item)
	}

	override fun onActivityResult(result: Boolean) {
		if (result) {
			presenter.savePage(
				resolver = contentResolver,
				page = reader?.currentPage ?: return
			)
		}
	}

	override fun saveState(chapterId: Long, page: Int, scroll: Int) {
		state = state.copy(chapterId = chapterId, page = page, scroll = scroll)
		ReaderPresenter.saveState(state)
	}

	override fun onLoadingStateChanged(isLoading: Boolean) {
		val hasPages = reader?.hasItems == true
		layout_loading.isVisible = isLoading && !hasPages
		progressBar_bottom.isVisible = isLoading && hasPages
	}

	override fun onError(e: Throwable) {
		val dialog = MaterialAlertDialogBuilder(this)
			.setTitle(R.string.error_occurred)
			.setMessage(e.message)
			.setPositiveButton(R.string.close, null)
		if (reader?.hasItems != true) {
			dialog.setOnDismissListener {
				finish()
			}
		}
		dialog.show()
	}

	override fun onGridTouch(area: Int) {
		when (area) {
			GridTouchHelper.AREA_CENTER -> {
				setUiIsVisible(!appbar_top.isVisible)
			}
			GridTouchHelper.AREA_TOP,
			GridTouchHelper.AREA_LEFT -> if (isTapSwitchEnabled) {
				reader?.switchPageBy(-1)
			}
			GridTouchHelper.AREA_BOTTOM,
			GridTouchHelper.AREA_RIGHT -> if (isTapSwitchEnabled) {
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

	override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean = when (keyCode) {
		KeyEvent.KEYCODE_VOLUME_UP -> if (isVolumeKeysSwitchEnabled) {
			reader?.switchPageBy(-1)
			true
		} else {
			super.onKeyDown(keyCode, event)
		}
		KeyEvent.KEYCODE_VOLUME_DOWN -> if (isVolumeKeysSwitchEnabled) {
			reader?.switchPageBy(1)
			true
		} else {
			super.onKeyDown(keyCode, event)
		}
		KeyEvent.KEYCODE_SPACE,
		KeyEvent.KEYCODE_PAGE_DOWN,
		KeyEvent.KEYCODE_DPAD_DOWN,
		KeyEvent.KEYCODE_DPAD_RIGHT -> {
			reader?.switchPageBy(1)
			true
		}
		KeyEvent.KEYCODE_PAGE_UP,
		KeyEvent.KEYCODE_DPAD_UP,
		KeyEvent.KEYCODE_DPAD_LEFT -> {
			reader?.switchPageBy(-1)
			true
		}
		KeyEvent.KEYCODE_DPAD_CENTER -> {
			setUiIsVisible(!appbar_top.isVisible)
			true
		}
		else -> super.onKeyDown(keyCode, event)
	}

	override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
		return (isVolumeKeysSwitchEnabled &&
				(keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP))
				|| super.onKeyUp(keyCode, event)
	}

	override fun onChapterChanged(chapter: MangaChapter) {
		state = state.copy(
			chapterId = chapter.id,
			page = 0,
			scroll = 0
		)
		reader?.updateState(chapterId = chapter.id)
	}

	override fun onPageSelected(page: MangaPage) {
		reader?.updateState(pageId = page.id)
	}

	override fun onReaderModeChanged(mode: ReaderMode) {
		//TODO save state
		presenter.setMode(state.manga, mode)
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
		title = chapter.name
		state.manga.chapters?.run {
			supportActionBar?.subtitle =
				getString(R.string.chapter_d_of_d, chapter.number, size)
		}
	}

	override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
		loadSettings()
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

	override fun onApplyWindowInsets(v: View, insets: WindowInsets): WindowInsets {
		appbar_top.updatePadding(top = insets.systemWindowInsetTop)
		appbar_bottom.updatePadding(bottom = insets.systemWindowInsetBottom)
		return insets.consumeSystemWindowInsets()
	}

	private fun loadSettings() {
		settings.readerPageSwitch.let {
			isTapSwitchEnabled = it.contains(AppSettings.PAGE_SWITCH_TAPS)
			isVolumeKeysSwitchEnabled = it.contains(AppSettings.PAGE_SWITCH_VOLUME_KEYS)
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
				page = 0,
				scroll = 0
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
						page = history.page,
						scroll = history.scroll
					)
				)
			}
	}
}