package org.koitharu.kotatsu.reader.ui.config

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.activity.result.ActivityResultCallback
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
import kotlinx.coroutines.plus
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.ReaderMode
import org.koitharu.kotatsu.core.prefs.observeAsStateFlow
import org.koitharu.kotatsu.core.ui.sheet.BaseAdaptiveSheet
import org.koitharu.kotatsu.core.util.ScreenOrientationHelper
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.showDistinct
import org.koitharu.kotatsu.core.util.ext.viewLifecycleScope
import org.koitharu.kotatsu.core.util.ext.withArgs
import org.koitharu.kotatsu.databinding.SheetReaderConfigBinding
import org.koitharu.kotatsu.reader.ui.PageSaveContract
import org.koitharu.kotatsu.reader.ui.ReaderViewModel
import org.koitharu.kotatsu.reader.ui.colorfilter.ColorFilterConfigActivity
import org.koitharu.kotatsu.settings.SettingsActivity
import javax.inject.Inject

@AndroidEntryPoint
class ReaderConfigSheet :
	BaseAdaptiveSheet<SheetReaderConfigBinding>(),
	ActivityResultCallback<Uri?>,
	View.OnClickListener,
	MaterialButtonToggleGroup.OnButtonCheckedListener,
	Slider.OnChangeListener,
	CompoundButton.OnCheckedChangeListener {

	private val viewModel by activityViewModels<ReaderViewModel>()
	private val savePageRequest = registerForActivityResult(PageSaveContract(), this)
	private var orientationHelper: ScreenOrientationHelper? = null
	private lateinit var mode: ReaderMode

	@Inject
	lateinit var settings: AppSettings

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		mode = arguments?.getInt(ARG_MODE)
			?.let { ReaderMode.valueOf(it) }
			?: ReaderMode.STANDARD
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

		binding.checkableGroup.addOnButtonCheckedListener(this)
		binding.buttonSavePage.setOnClickListener(this)
		binding.buttonScreenRotate.setOnClickListener(this)
		binding.buttonSettings.setOnClickListener(this)
		binding.buttonColorFilter.setOnClickListener(this)
		binding.sliderTimer.addOnChangeListener(this)
		binding.switchScrollTimer.setOnCheckedChangeListener(this)

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
		findCallback()?.run {
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
				val page = viewModel.getCurrentPage() ?: return
				viewModel.saveCurrentPage(page, savePageRequest)
			}

			R.id.button_screen_rotate -> {
				orientationHelper?.toggleOrientation()
			}

			R.id.button_color_filter -> {
				val page = viewModel.getCurrentPage() ?: return
				val manga = viewModel.manga?.any ?: return
				startActivity(ColorFilterConfigActivity.newIntent(v.context, manga, page))
			}
		}
	}

	override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
		when (buttonView.id) {
			R.id.switch_scroll_timer -> {
				findCallback()?.isAutoScrollEnabled = isChecked
				requireViewBinding().labelTimer.isVisible = isChecked
				requireViewBinding().sliderTimer.isVisible = isChecked
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
			else -> return
		}
		if (newMode == mode) {
			return
		}
		findCallback()?.onReaderModeChanged(newMode) ?: return
		mode = newMode
	}

	override fun onValueChange(slider: Slider, value: Float, fromUser: Boolean) {
		if (fromUser) {
			settings.readerAutoscrollSpeed = value
		}
	}

	override fun onActivityResult(result: Uri?) {
		viewModel.onActivityResult(result)
		dismissAllowingStateLoss()
	}

	private fun observeScreenOrientation() {
		val helper = ScreenOrientationHelper(requireActivity())
		orientationHelper = helper
		helper.observeAutoOrientation()
			.onEach {
				requireViewBinding().buttonScreenRotate.isGone = it
			}.launchIn(viewLifecycleScope)
	}

	private fun findCallback(): Callback? {
		return (parentFragment as? Callback) ?: (activity as? Callback)
	}

	interface Callback {

		var isAutoScrollEnabled: Boolean

		fun onReaderModeChanged(mode: ReaderMode)
	}

	companion object {

		private const val TAG = "ReaderConfigBottomSheet"
		private const val ARG_MODE = "mode"

		fun show(fm: FragmentManager, mode: ReaderMode) = ReaderConfigSheet().withArgs(1) {
			putInt(ARG_MODE, mode.id)
		}.showDistinct(fm, TAG)
	}
}
