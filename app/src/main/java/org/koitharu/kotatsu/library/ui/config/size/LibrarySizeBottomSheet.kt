package org.koitharu.kotatsu.library.ui.config.size

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.google.android.material.slider.LabelFormatter
import com.google.android.material.slider.Slider
import org.koin.android.ext.android.inject
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseBottomSheet
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.databinding.SheetLibrarySizeBinding
import org.koitharu.kotatsu.utils.ext.setValueRounded
import org.koitharu.kotatsu.utils.progress.IntPercentLabelFormatter

class LibrarySizeBottomSheet :
	BaseBottomSheet<SheetLibrarySizeBinding>(),
	Slider.OnChangeListener,
	View.OnClickListener {

	private val settings by inject<AppSettings>(mode = LazyThreadSafetyMode.NONE)
	private var labelFormatter: LabelFormatter? = null

	override fun onInflateView(inflater: LayoutInflater, container: ViewGroup?): SheetLibrarySizeBinding {
		return SheetLibrarySizeBinding.inflate(inflater, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		labelFormatter = IntPercentLabelFormatter(view.context)
		binding.sliderGrid.addOnChangeListener(this)
		binding.buttonSmall.setOnClickListener(this)
		binding.buttonLarge.setOnClickListener(this)

		binding.sliderGrid.setValueRounded(settings.gridSize.toFloat())
	}

	override fun onDestroyView() {
		labelFormatter = null
		super.onDestroyView()
	}

	override fun onValueChange(slider: Slider, value: Float, fromUser: Boolean) {
		settings.gridSize = value.toInt()
		binding.textViewLabel.text = labelFormatter?.getFormattedValue(value)
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.button_small -> binding.sliderGrid.value -= binding.sliderGrid.stepSize
			R.id.button_large -> binding.sliderGrid.value += binding.sliderGrid.stepSize
		}
	}

	companion object {

		private const val TAG = "LibrarySizeBottomSheet"

		fun show(fm: FragmentManager) = LibrarySizeBottomSheet().show(fm, TAG)
	}
}
