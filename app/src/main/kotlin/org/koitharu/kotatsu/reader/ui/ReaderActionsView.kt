package org.koitharu.kotatsu.reader.ui

import android.content.Context
import android.content.SharedPreferences
import android.database.ContentObserver
import android.provider.Settings
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.annotation.AttrRes
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.google.android.material.slider.Slider
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.nav.AppRouter
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.ReaderControl
import org.koitharu.kotatsu.core.util.ext.hasVisibleChildren
import org.koitharu.kotatsu.core.util.ext.isRtl
import org.koitharu.kotatsu.core.util.ext.setContentDescriptionAndTooltip
import org.koitharu.kotatsu.core.util.ext.setTooltipCompat
import org.koitharu.kotatsu.core.util.ext.setValueRounded
import org.koitharu.kotatsu.databinding.LayoutReaderActionsBinding
import org.koitharu.kotatsu.details.ui.pager.ChaptersPagesSheet
import org.koitharu.kotatsu.details.ui.pager.ChaptersPagesSheet.Companion.TAB_PAGES
import org.koitharu.kotatsu.reader.ui.ReaderControlDelegate.OnInteractionListener
import javax.inject.Inject
import com.google.android.material.R as materialR

@AndroidEntryPoint
class ReaderActionsView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	@AttrRes defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr),
	View.OnClickListener,
	SharedPreferences.OnSharedPreferenceChangeListener,
	Slider.OnChangeListener,
	Slider.OnSliderTouchListener, View.OnLongClickListener {

	@Inject
	lateinit var settings: AppSettings

	private val binding = LayoutReaderActionsBinding.inflate(LayoutInflater.from(context), this)
	private val rotationObserver = object : ContentObserver(handler) {
		override fun onChange(selfChange: Boolean) {
			post {
				updateRotationButton()
			}
		}
	}
	private var isSliderChanged = false
	private var isSliderTracking = false

	var isSliderEnabled: Boolean
		get() = binding.slider.isEnabled
		set(value) {
			binding.slider.isEnabled = value
			binding.slider.setThumbVisible(value)
		}

	var isNextEnabled: Boolean
		get() = binding.buttonNext.isEnabled
		set(value) {
			binding.buttonNext.isEnabled = value
		}

	var isPrevEnabled: Boolean
		get() = binding.buttonPrev.isEnabled
		set(value) {
			binding.buttonPrev.isEnabled = value
		}

	var isBookmarkAdded: Boolean = false
		set(value) {
			if (field != value) {
				field = value
				updateBookmarkButton()
			}
		}

	var listener: OnInteractionListener? = null

	init {
		orientation = HORIZONTAL
		gravity = Gravity.CENTER_VERTICAL
		binding.buttonNext.initAction()
		binding.buttonPrev.initAction()
		binding.buttonSave.initAction()
		binding.buttonOptions.initAction()
		binding.buttonScreenRotation.initAction()
		binding.buttonPagesThumbs.initAction()
		binding.buttonTimer.initAction()
		binding.buttonBookmark.initAction()
		binding.slider.setLabelFormatter(PageLabelFormatter())
		binding.slider.addOnChangeListener(this)
		binding.slider.addOnSliderTouchListener(this)
		updateControlsVisibility()
		updatePagesSheetButton()
		updateRotationButton()
	}

	override fun onAttachedToWindow() {
		super.onAttachedToWindow()
		settings.subscribe(this)
		context.contentResolver.registerContentObserver(
			Settings.System.CONTENT_URI, true, rotationObserver,
		)
	}

	override fun onDetachedFromWindow() {
		settings.unsubscribe(this)
		context.contentResolver.unregisterContentObserver(rotationObserver)
		super.onDetachedFromWindow()
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.button_prev -> listener?.switchChapterBy(-1)
			R.id.button_next -> listener?.switchChapterBy(1)
			R.id.button_save -> listener?.onSavePageClick()
			R.id.button_timer -> listener?.onScrollTimerClick(isLongClick = false)
			R.id.button_pages_thumbs -> AppRouter.from(this)?.showChapterPagesSheet()
			R.id.button_screen_rotation -> listener?.toggleScreenOrientation()
			R.id.button_options -> listener?.openMenu()
			R.id.button_bookmark -> listener?.onBookmarkClick()
		}
	}

	override fun onLongClick(v: View): Boolean = when (v.id) {
		R.id.button_bookmark -> AppRouter.from(this)
			?.showChapterPagesSheet(ChaptersPagesSheet.TAB_BOOKMARKS)

		R.id.button_timer -> listener?.onScrollTimerClick(isLongClick = true)
		R.id.button_options -> AppRouter.from(this)?.openReaderSettings()
		else -> null
	} != null

	override fun onValueChange(slider: Slider, value: Float, fromUser: Boolean) {
		if (fromUser) {
			if (isSliderTracking) {
				isSliderChanged = true
			} else {
				listener?.switchPageTo(value.toInt())
			}
		}
	}

	override fun onStartTrackingTouch(slider: Slider) {
		if (!isSliderTracking) {
			isSliderChanged = false
			isSliderTracking = true
		}
	}

	override fun onStopTrackingTouch(slider: Slider) {
		isSliderTracking = false
		if (isSliderChanged) {
			listener?.switchPageTo(slider.value.toInt())
		}
	}

	override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
		when (key) {
			AppSettings.KEY_READER_CONTROLS -> updateControlsVisibility()
			AppSettings.KEY_PAGES_TAB,
			AppSettings.KEY_DETAILS_TAB,
			AppSettings.KEY_DETAILS_LAST_TAB -> updatePagesSheetButton()
		}
	}

	fun setSliderValue(value: Int, max: Int) {
		binding.slider.valueTo = max.toFloat()
		binding.slider.setValueRounded(value.toFloat())
	}

	fun setSliderReversed(reversed: Boolean) {
		binding.slider.isRtl = reversed != isRtl
	}

	fun setTimerActive(isActive: Boolean) {
		binding.buttonTimer.setIconResource(
			if (isActive) R.drawable.ic_timer_run else R.drawable.ic_timer,
		)
	}

	private fun updateControlsVisibility() {
		val controls = settings.readerControls
		binding.buttonPrev.isVisible = ReaderControl.PREV_CHAPTER in controls
		binding.buttonNext.isVisible = ReaderControl.NEXT_CHAPTER in controls
		binding.buttonPagesThumbs.isVisible = ReaderControl.PAGES_SHEET in controls
		binding.buttonScreenRotation.isVisible = ReaderControl.SCREEN_ROTATION in controls
		binding.buttonSave.isVisible = ReaderControl.SAVE_PAGE in controls
		binding.buttonTimer.isVisible = ReaderControl.TIMER in controls
		binding.buttonBookmark.isVisible = ReaderControl.BOOKMARK in controls
		binding.slider.isVisible = ReaderControl.SLIDER in controls
		adjustLayoutParams()
	}

	private fun updatePagesSheetButton() {
		val isPagesMode = settings.defaultDetailsTab == TAB_PAGES
		val button = binding.buttonPagesThumbs
		button.setIconResource(
			if (isPagesMode) R.drawable.ic_grid else R.drawable.ic_list,
		)
		button.setContentDescriptionAndTooltip(
			if (isPagesMode) R.string.pages else R.string.chapters,
		)
	}

	private fun updateBookmarkButton() {
		val button = binding.buttonBookmark
		button.setIconResource(
			if (isBookmarkAdded) R.drawable.ic_bookmark_added else R.drawable.ic_bookmark,
		)
		button.setContentDescriptionAndTooltip(
			if (isBookmarkAdded) R.string.bookmark_remove else R.string.bookmark_add,
		)
	}

	private fun adjustLayoutParams() {
		val isSliderVisible = binding.slider.isVisible
		repeat(childCount) { i ->
			val child = getChildAt(i)
			if (child is FrameLayout) {
				child.isVisible = child.hasVisibleChildren
				child.updateLayoutParams<LayoutParams> {
					width = if (isSliderVisible) LayoutParams.WRAP_CONTENT else 0
					weight = if (isSliderVisible) 0f else 1f
				}
			}
		}
	}

	private fun updateRotationButton() {
		val button = binding.buttonScreenRotation
		when {
			!button.isVisible -> return
			isAutoRotationEnabled() -> {
				button.setContentDescriptionAndTooltip(R.string.lock_screen_rotation)
				button.setIconResource(R.drawable.ic_screen_rotation_lock)
			}

			else -> {
				button.setContentDescriptionAndTooltip(R.string.rotate_screen)
				button.setIconResource(R.drawable.ic_screen_rotation)
			}
		}
	}

	private fun Button.initAction() {
		setOnClickListener(this@ReaderActionsView)
		setOnLongClickListener(this@ReaderActionsView)
		setTooltipCompat(contentDescription)
	}

	private fun isAutoRotationEnabled(): Boolean = Settings.System.getInt(
		context.contentResolver,
		Settings.System.ACCELEROMETER_ROTATION,
		0,
	) == 1

	private fun Slider.setThumbVisible(visible: Boolean) {
		thumbWidth = if (visible) {
			resources.getDimensionPixelSize(materialR.dimen.m3_comp_slider_active_handle_width)
		} else {
			0
		}
		thumbHeight = if (visible) {
			resources.getDimensionPixelSize(materialR.dimen.m3_comp_slider_active_handle_height)
		} else {
			0
		}
	}
}
