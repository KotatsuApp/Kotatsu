package org.koitharu.kotatsu.reader.ui.config

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.activityViewModels
import androidx.transition.TransitionManager
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.slider.Slider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.nav.AppRouter
import org.koitharu.kotatsu.core.nav.router
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.ReaderMode
import org.koitharu.kotatsu.core.ui.sheet.BaseAdaptiveSheet
import org.koitharu.kotatsu.core.util.ext.consume
import org.koitharu.kotatsu.core.util.ext.findParentCallback
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.setValueRounded
import org.koitharu.kotatsu.core.util.ext.viewLifecycleScope
import org.koitharu.kotatsu.core.util.progress.IntPercentLabelFormatter
import org.koitharu.kotatsu.databinding.SheetReaderConfigBinding
import org.koitharu.kotatsu.reader.domain.PageLoader
import org.koitharu.kotatsu.reader.ui.ReaderViewModel
import org.koitharu.kotatsu.reader.ui.ScreenOrientationHelper
import javax.inject.Inject

@AndroidEntryPoint
class ReaderConfigSheet :
	BaseAdaptiveSheet<SheetReaderConfigBinding>(),
	View.OnClickListener,
	MaterialButtonToggleGroup.OnButtonCheckedListener,
	CompoundButton.OnCheckedChangeListener,
	Slider.OnChangeListener {

	private val viewModel by activityViewModels<ReaderViewModel>()

	@Inject
	lateinit var orientationHelper: ScreenOrientationHelper

	@Inject
	lateinit var mangaRepositoryFactory: MangaRepository.Factory

	@Inject
	lateinit var pageLoader: PageLoader

	private lateinit var mode: ReaderMode
	private lateinit var imageServerDelegate: ImageServerDelegate

	@Inject
	lateinit var settings: AppSettings

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		mode = arguments?.getInt(AppRouter.KEY_READER_MODE)
			?.let { ReaderMode.valueOf(it) }
			?: ReaderMode.STANDARD
		imageServerDelegate = ImageServerDelegate(
			mangaRepositoryFactory = mangaRepositoryFactory,
			mangaSource = viewModel.getMangaOrNull()?.source,
		)
	}

	override fun onCreateViewBinding(
		inflater: LayoutInflater,
		container: ViewGroup?,
	): SheetReaderConfigBinding {
		return SheetReaderConfigBinding.inflate(inflater, container, false)
	}

	override fun onViewBindingCreated(
		binding: SheetReaderConfigBinding,
		savedInstanceState: Bundle?,
	) {
		super.onViewBindingCreated(binding, savedInstanceState)
		observeScreenOrientation()
		binding.buttonStandard.isChecked = mode == ReaderMode.STANDARD
		binding.buttonReversed.isChecked = mode == ReaderMode.REVERSED
		binding.buttonWebtoon.isChecked = mode == ReaderMode.WEBTOON
		binding.buttonVertical.isChecked = mode == ReaderMode.VERTICAL
		binding.switchDoubleReader.isChecked = settings.isReaderDoubleOnLandscape
		binding.switchDoubleReader.isEnabled = mode == ReaderMode.STANDARD || mode == ReaderMode.REVERSED
		binding.sliderDoubleSensitivity.setValueRounded(settings.readerDoublePagesSensitivity * 100f)
		binding.sliderDoubleSensitivity.setLabelFormatter(IntPercentLabelFormatter(binding.root.context))
        binding.adjustSensitivitySlider(withAnimation = false)

		binding.checkableGroup.addOnButtonCheckedListener(this)
		binding.buttonSavePage.setOnClickListener(this)
		binding.buttonScreenRotate.setOnClickListener(this)
		binding.buttonSettings.setOnClickListener(this)
		binding.buttonImageServer.setOnClickListener(this)
		binding.buttonColorFilter.setOnClickListener(this)
		binding.buttonScrollTimer.setOnClickListener(this)
		binding.buttonBookmark.setOnClickListener(this)
		binding.switchDoubleReader.setOnCheckedChangeListener(this)
		binding.sliderDoubleSensitivity.addOnChangeListener(this)

		viewModel.isBookmarkAdded.observe(viewLifecycleOwner) {
			binding.buttonBookmark.setText(if (it) R.string.bookmark_remove else R.string.bookmark_add)
			binding.buttonBookmark.setCompoundDrawablesRelativeWithIntrinsicBounds(
				if (it) R.drawable.ic_bookmark_checked else R.drawable.ic_bookmark, 0, 0, 0,
			)
		}

		viewLifecycleScope.launch {
			val isAvailable = imageServerDelegate.isAvailable()
			if (isAvailable) {
				bindImageServerTitle()
			}
			binding.buttonImageServer.isVisible = isAvailable
		}
	}

	override fun onApplyWindowInsets(v: View, insets: WindowInsetsCompat): WindowInsetsCompat {
		val typeMask = WindowInsetsCompat.Type.systemBars()
		viewBinding?.scrollView?.updatePadding(
			bottom = insets.getInsets(typeMask).bottom,
		)
		return insets.consume(v, typeMask, bottom = true)
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.button_settings -> {
				router.openReaderSettings()
				dismissAllowingStateLoss()
			}

			R.id.button_scroll_timer -> {
				findParentCallback(Callback::class.java)?.onScrollTimerClick(false) ?: return
				dismissAllowingStateLoss()
			}

			R.id.button_save_page -> {
				findParentCallback(Callback::class.java)?.onSavePageClick() ?: return
				dismissAllowingStateLoss()
			}

			R.id.button_screen_rotate -> {
				orientationHelper.isLandscape = !orientationHelper.isLandscape
			}

			R.id.button_bookmark -> {
				viewModel.toggleBookmark()
			}

			R.id.button_color_filter -> {
				val page = viewModel.getCurrentPage() ?: return
				val manga = viewModel.getMangaOrNull() ?: return
				router.openColorFilterConfig(manga, page)
			}

			R.id.button_image_server -> viewLifecycleScope.launch {
				if (imageServerDelegate.showDialog(v.context)) {
					bindImageServerTitle()
					pageLoader.invalidate(clearCache = true)
					viewModel.switchChapterBy(0)
				}
			}
		}
	}

	override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
		when (buttonView.id) {
			R.id.switch_screen_lock_rotation -> {
				orientationHelper.isLocked = isChecked
			}

			R.id.switch_double_reader -> {
				settings.isReaderDoubleOnLandscape = isChecked
				viewBinding?.adjustSensitivitySlider(withAnimation = true)
				findParentCallback(Callback::class.java)?.onDoubleModeChanged(isChecked)
			}
		}
	}

	override fun onValueChange(slider: Slider, value: Float, fromUser: Boolean) {
		settings.readerDoublePagesSensitivity = value / 100f
	}

	override fun onButtonChecked(
		group: MaterialButtonToggleGroup?,
		checkedId: Int,
		isChecked: Boolean,
	) {
		if (!isChecked) {
			return
		}
		val newMode = when (checkedId) {
			R.id.button_standard -> ReaderMode.STANDARD
			R.id.button_webtoon -> ReaderMode.WEBTOON
			R.id.button_reversed -> ReaderMode.REVERSED
			R.id.button_vertical -> ReaderMode.VERTICAL
			else -> return
		}
		viewBinding?.run {
            switchDoubleReader.isEnabled = newMode == ReaderMode.STANDARD || newMode == ReaderMode.REVERSED
            adjustSensitivitySlider(withAnimation = true)
        }
		if (newMode == mode) {
			return
		}
		findParentCallback(Callback::class.java)?.onReaderModeChanged(newMode) ?: return
		mode = newMode
	}

	private fun observeScreenOrientation() {
		orientationHelper.observeAutoOrientation()
			.onEach {
				with(requireViewBinding()) {
					buttonScreenRotate.isGone = it
					switchScreenLockRotation.isVisible = it
					updateOrientationLockSwitch()
				}
			}.launchIn(viewLifecycleScope)
	}

	private fun updateOrientationLockSwitch() {
		val switch = viewBinding?.switchScreenLockRotation ?: return
		switch.setOnCheckedChangeListener(null)
		switch.isChecked = orientationHelper.isLocked
		switch.setOnCheckedChangeListener(this)
	}

	private suspend fun bindImageServerTitle() {
		viewBinding?.buttonImageServer?.text = getString(
			R.string.inline_preference_pattern,
			getString(R.string.image_server),
			imageServerDelegate.getValue() ?: getString(R.string.automatic),
		)
	}

    private fun SheetReaderConfigBinding.adjustSensitivitySlider(withAnimation: Boolean) {
        val isSliderVisible = switchDoubleReader.isEnabled && switchDoubleReader.isChecked
        if (isSliderVisible != sliderDoubleSensitivity.isVisible && withAnimation) {
            TransitionManager.beginDelayedTransition(layoutMain)
        }
        sliderDoubleSensitivity.isVisible = isSliderVisible
        textDoubleSensitivity.isVisible = isSliderVisible
    }

	interface Callback {

		fun onReaderModeChanged(mode: ReaderMode)

		fun onDoubleModeChanged(isEnabled: Boolean)

		fun onSavePageClick()

		fun onScrollTimerClick(isLongClick: Boolean)

		fun onBookmarkClick()
	}
}
