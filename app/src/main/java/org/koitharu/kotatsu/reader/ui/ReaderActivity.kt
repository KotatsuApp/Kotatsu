package org.koitharu.kotatsu.reader.ui

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
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.graphics.Insets
import androidx.core.view.*
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseFullscreenActivity
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.model.MangaChapter
import org.koitharu.kotatsu.core.model.MangaHistory
import org.koitharu.kotatsu.core.model.MangaPage
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.ReaderMode
import org.koitharu.kotatsu.databinding.ActivityReaderBinding
import org.koitharu.kotatsu.reader.ui.base.AbstractReader
import org.koitharu.kotatsu.reader.ui.reversed.ReversedReaderFragment
import org.koitharu.kotatsu.reader.ui.standard.PagerReaderFragment
import org.koitharu.kotatsu.reader.ui.thumbnails.OnPageSelectListener
import org.koitharu.kotatsu.reader.ui.thumbnails.PagesThumbnailsSheet
import org.koitharu.kotatsu.reader.ui.wetoon.WebtoonReaderFragment
import org.koitharu.kotatsu.utils.GridTouchHelper
import org.koitharu.kotatsu.utils.MangaShortcut
import org.koitharu.kotatsu.utils.ScreenOrientationHelper
import org.koitharu.kotatsu.utils.ShareHelper
import org.koitharu.kotatsu.utils.anim.Motion
import org.koitharu.kotatsu.utils.ext.*

class ReaderActivity : BaseFullscreenActivity<ActivityReaderBinding>(),
	ChaptersDialog.OnChapterChangeListener,
	GridTouchHelper.OnGridTouchListener, OnPageSelectListener, ReaderConfigDialog.Callback,
	ReaderListener, SharedPreferences.OnSharedPreferenceChangeListener,
	ActivityResultCallback<Boolean>, OnApplyWindowInsetsListener {

	private val viewModel by viewModel<ReaderViewModel>()
	private val settings by inject<AppSettings>()

	lateinit var state: ReaderState
		private set

	private lateinit var touchHelper: GridTouchHelper
	private lateinit var orientationHelper: ScreenOrientationHelper
	private var isTapSwitchEnabled = true
	private var isVolumeKeysSwitchEnabled = false

	private val reader
		get() = supportFragmentManager.findFragmentById(R.id.container) as? AbstractReader<*>

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityReaderBinding.inflate(layoutInflater))
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		touchHelper = GridTouchHelper(this, this)
		orientationHelper = ScreenOrientationHelper(this)
		binding.toolbarBottom.inflateMenu(R.menu.opt_reader_bottom)
		binding.toolbarBottom.setOnMenuItemClickListener(::onOptionsItemSelected)

		@Suppress("RemoveExplicitTypeArguments")
		state = savedInstanceState?.getParcelable<ReaderState>(EXTRA_STATE)
			?: intent.getParcelableExtra<ReaderState>(EXTRA_STATE)
					?: let {
				Toast.makeText(this, R.string.error_occurred, Toast.LENGTH_SHORT).show()
				finishAfterTransition()
				return
			}

		title = state.chapter?.name ?: state.manga.title
		state.manga.chapters?.run {
			supportActionBar?.subtitle =
				getString(R.string.chapter_d_of_d, state.chapter?.number ?: 0, size)
		}

		ViewCompat.setOnApplyWindowInsetsListener(binding.rootLayout, this)

		settings.subscribe(this)
		loadSwitchSettings()
		orientationHelper.observeAutoOrientation()
			.onEach {
				binding.toolbarBottom.menu.findItem(R.id.action_screen_rotate).isVisible = !it
			}.launchIn(lifecycleScope)

		if (savedInstanceState == null) {
			viewModel.init(state.manga)
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
				GlobalScope.launch(Dispatchers.Main + IgnoreErrors) {
					MangaShortcut(state.manga).addAppShortcut(applicationContext)
				}
			}
		}

		viewModel.onError.observe(this, this::onError)
		viewModel.reader.observe(this) { (manga, mode) -> onInitReader(manga, mode) }
		viewModel.onPageSaved.observe(this, this::onPageSaved)
	}

	private fun onInitReader(manga: Manga, mode: ReaderMode) {
		val currentReader = reader
		when (mode) {
			ReaderMode.WEBTOON -> if (currentReader !is WebtoonReaderFragment) {
				supportFragmentManager.commit {
					replace(R.id.container, WebtoonReaderFragment.newInstance(state))
				}
			}
			ReaderMode.REVERSED -> if (currentReader !is ReversedReaderFragment) {
				supportFragmentManager.commit {
					replace(R.id.container, ReversedReaderFragment.newInstance(state))
				}
			}
			ReaderMode.STANDARD -> if (currentReader !is PagerReaderFragment) {
				supportFragmentManager.commit {
					replace(R.id.container, PagerReaderFragment.newInstance(state))
				}
			}
		}
		binding.toolbarBottom.menu.findItem(R.id.action_reader_mode).setIcon(
			when (mode) {
				ReaderMode.WEBTOON -> R.drawable.ic_script
				ReaderMode.REVERSED -> R.drawable.ic_read_reversed
				ReaderMode.STANDARD -> R.drawable.ic_book_page
			}
		)
		binding.appbarTop.postDelayed(1000) {
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

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		when (item.itemId) {
			R.id.action_reader_mode -> {
				ReaderConfigDialog.show(
					supportFragmentManager, when (reader) {
						is PagerReaderFragment -> ReaderMode.STANDARD
						is WebtoonReaderFragment -> ReaderMode.WEBTOON
						is ReversedReaderFragment -> ReaderMode.REVERSED
						else -> {
							showWaitWhileLoading()
							return false
						}
					}
				)
			}
			R.id.action_settings -> {
				startActivity(SimpleSettingsActivity.newReaderSettingsIntent(this))
			}
			R.id.action_chapters -> {
				ChaptersDialog.show(
					supportFragmentManager,
					state.manga.chapters.orEmpty(),
					state.chapterId
				)
			}
			R.id.action_screen_rotate -> {
				orientationHelper.toggleOrientation()
			}
			R.id.action_pages_thumbs -> {
				if (reader?.hasItems == true) {
					val pages = reader?.getPages()
					if (!pages.isNullOrEmpty()) {
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
			}
			else -> return super.onOptionsItemSelected(item)
		}
		return true
	}

	override fun onActivityResult(result: Boolean) {
		if (result) {
			viewModel.savePage(
				resolver = contentResolver,
				page = reader?.currentPage ?: return
			)
		}
	}

	override fun saveState(chapterId: Long, page: Int, scroll: Int) {
		state = state.copy(chapterId = chapterId, page = page, scroll = scroll)
		ReaderViewModel.saveState(state)
	}

	override fun onLoadingStateChanged(isLoading: Boolean) {
		val hasPages = reader?.hasItems == true
		binding.layoutLoading.isVisible = isLoading && !hasPages
		binding.progressBarBottom.isVisible = isLoading && hasPages
	}

	override fun onError(e: Throwable) {
		val dialog = AlertDialog.Builder(this)
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
				setUiIsVisible(!binding.appbarTop.isVisible)
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
		return if (binding.appbarTop.hasGlobalPoint(rawX, rawY)
			|| binding.appbarBottom.hasGlobalPoint(rawX, rawY)
		) {
			false
		} else {
			val targets = binding.rootLayout.hitTest(rawX, rawY)
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
			setUiIsVisible(!binding.appbarTop.isVisible)
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
		viewModel.setMode(state.manga, mode)
	}

	private fun onPageSaved(uri: Uri?) {
		if (uri != null) {
			Snackbar.make(binding.container, R.string.page_saved, Snackbar.LENGTH_LONG)
				.setAnchorView(binding.appbarBottom)
				.setAction(R.string.share) {
					ShareHelper.shareImage(this, uri)
				}.show()
		} else {
			Snackbar.make(binding.container, R.string.error_occurred, Snackbar.LENGTH_SHORT)
				.setAnchorView(binding.appbarBottom)
				.show()
		}
	}

	override fun onPageChanged(chapter: MangaChapter, page: Int) {
		title = chapter.name
		state.manga.chapters?.run {
			supportActionBar?.subtitle =
				getString(R.string.chapter_d_of_d, chapter.number, size)
		}
	}

	override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
		when (key) {
			AppSettings.KEY_READER_SWITCHERS -> loadSwitchSettings()
			AppSettings.KEY_READER_ANIMATION,
			AppSettings.KEY_ZOOM_MODE -> reader?.recreateAdapter()
		}
	}

	private fun showWaitWhileLoading() {
		Toast.makeText(this, R.string.wait_for_loading_finish, Toast.LENGTH_SHORT).apply {
			setGravity(Gravity.CENTER, 0, 0)
		}.show()
	}

	private fun setUiIsVisible(isUiVisible: Boolean) {
		if (binding.appbarTop.isVisible != isUiVisible) {
			if (isUiVisible) {
				binding.appbarTop.showAnimated(Motion.SlideTop)
				binding.appbarBottom.showAnimated(Motion.SlideBottom)
				showSystemUI()
			} else {
				binding.appbarTop.hideAnimated(Motion.SlideTop)
				binding.appbarBottom.hideAnimated(Motion.SlideBottom)
				hideSystemUI()
			}
		}
	}

	override fun onApplyWindowInsets(v: View, insets: WindowInsetsCompat): WindowInsetsCompat {
		val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
		binding.appbarTop.updatePadding(
			top = systemBars.top,
			right = systemBars.right,
			left = systemBars.left
		)
		binding.appbarBottom.updatePadding(
			bottom = systemBars.bottom,
			right = systemBars.right,
			left = systemBars.left
		)
		return WindowInsetsCompat.Builder(insets)
			.setInsets(WindowInsetsCompat.Type.systemBars(), Insets.NONE)
			.build()
	}

	private fun loadSwitchSettings() {
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