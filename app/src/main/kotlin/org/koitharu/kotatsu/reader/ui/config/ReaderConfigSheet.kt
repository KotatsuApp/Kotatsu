package org.koitharu.kotatsu.reader.ui.config

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.slider.Slider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.ReaderMode
import org.koitharu.kotatsu.core.prefs.observeAsStateFlow
import org.koitharu.kotatsu.core.ui.sheet.BaseAdaptiveSheet
import org.koitharu.kotatsu.core.util.ext.findParentCallback
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.showDistinct
import org.koitharu.kotatsu.core.util.ext.viewLifecycleScope
import org.koitharu.kotatsu.core.util.ext.withArgs
import org.koitharu.kotatsu.databinding.SheetReaderConfigBinding
import org.koitharu.kotatsu.reader.domain.PageLoader
import org.koitharu.kotatsu.reader.ui.ReaderViewModel
import org.koitharu.kotatsu.reader.ui.ScreenOrientationHelper
import org.koitharu.kotatsu.reader.ui.colorfilter.ColorFilterConfigActivity
import org.koitharu.kotatsu.settings.SettingsActivity
import javax.inject.Inject

@AndroidEntryPoint
class ReaderConfigSheet :
	BaseAdaptiveSheet<SheetReaderConfigBinding>(),
	View.OnClickListener,
	MaterialButtonToggleGroup.OnButtonCheckedListener,
	Slider.OnChangeListener,
	CompoundButton.OnCheckedChangeListener {

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
		mode = arguments?.getInt(ARG_MODE)
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
		binding.switchDoubleReader.isEnabled = mode == ReaderMode.STANDARD

		binding.checkableGroup.addOnButtonCheckedListener(this)
		binding.buttonSavePage.setOnClickListener(this)
		binding.buttonScreenRotate.setOnClickListener(this)
		binding.buttonSettings.setOnClickListener(this)
		binding.buttonImageServer.setOnClickListener(this)
		binding.buttonColorFilter.setOnClickListener(this)
		binding.sliderTimer.addOnChangeListener(this)
		binding.switchScrollTimer.setOnCheckedChangeListener(this)
		binding.switchDoubleReader.setOnCheckedChangeListener(this)

		viewLifecycleScope.launch {
			val isAvailable = imageServerDelegate.isAvailable()
			if (isAvailable) {
				bindImageServerTitle()
			}
			binding.buttonImageServer.isVisible = isAvailable
		}

		settings.observeAsStateFlow(
			scope = lifecycleScope + Dispatchers.Default,
			key = AppSettings.KEY_READER_AUTOSCROLL_SPEED,
			valueProducer = { readerAutoscrollSpeed },
		).observe(viewLifecycleOwner) {
			binding.sliderTimer.value = it.coerceIn(
				binding.sliderTimer.valueFrom,
				binding.sliderTimer.valueTo,
			)
		}
		findParentCallback(Callback::class.java)?.run {
			binding.switchScrollTimer.isChecked = isAutoScrollEnabled
		}
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.button_settings -> {
				startActivity(SettingsActivity.newReaderSettingsIntent(v.context))
				dismissAllowingStateLoss()
			}

			R.id.button_save_page -> {
				findParentCallback(Callback::class.java)?.onSavePageClick() ?: return
				dismissAllowingStateLoss()
			}

			R.id.button_screen_rotate -> {
				orientationHelper.isLandscape = !orientationHelper.isLandscape
			}

			R.id.button_color_filter -> {
				val page = viewModel.getCurrentPage() ?: return
				val manga = viewModel.getMangaOrNull() ?: return
				startActivity(ColorFilterConfigActivity.newIntent(v.context, manga, page))
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
			R.id.switch_scroll_timer -> {
				findParentCallback(Callback::class.java)?.isAutoScrollEnabled = isChecked
				requireViewBinding().layoutTimer.isVisible = isChecked
				requireViewBinding().sliderTimer.isVisible = isChecked
			}

			R.id.switch_screen_lock_rotation -> {
				orientationHelper.isLocked = isChecked
			}

			R.id.switch_double_reader -> {
				settings.isReaderDoubleOnLandscape = isChecked
				findParentCallback(Callback::class.java)?.onDoubleModeChanged(isChecked)
			}
		}
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
		viewBinding?.switchDoubleReader?.isEnabled = newMode == ReaderMode.STANDARD || newMode == ReaderMode.REVERSED
		if (newMode == mode) {
			return
		}
		findParentCallback(Callback::class.java)?.onReaderModeChanged(newMode) ?: return
		mode = newMode
	}

	override fun onValueChange(slider: Slider, value: Float, fromUser: Boolean) {
		if (fromUser) {
			settings.readerAutoscrollSpeed = value
		}
		(viewBinding ?: return).labelTimerValue.text = getString(R.string.speed_value, value * 10f)
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

	interface Callback {

		var isAutoScrollEnabled: Boolean

		fun onReaderModeChanged(mode: ReaderMode)

		fun onDoubleModeChanged(isEnabled: Boolean)

		fun onSavePageClick()
	}

	companion object {

		private const val TAG = "ReaderConfigBottomSheet"
		private const val ARG_MODE = "mode"

		fun show(fm: FragmentManager, mode: ReaderMode) = ReaderConfigSheet().withArgs(1) {
			putInt(ARG_MODE, mode.id)
		}.showDistinct(fm, TAG)
	}
}
