package org.koitharu.kotatsu.shelf.ui.config.size

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.google.android.material.slider.LabelFormatter
import com.google.android.material.slider.Slider
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseBottomSheet
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.databinding.SheetShelfSizeBinding
import org.koitharu.kotatsu.utils.ext.setValueRounded
import org.koitharu.kotatsu.utils.progress.IntPercentLabelFormatter
import javax.inject.Inject

@AndroidEntryPoint
class ShelfSizeBottomSheet :
	BaseBottomSheet<SheetShelfSizeBinding>(),
	Slider.OnChangeListener,
	View.OnClickListener {

	@Inject
	lateinit var settings: AppSettings
	private var labelFormatter: LabelFormatter? = null

	override fun onInflateView(inflater: LayoutInflater, container: ViewGroup?): SheetShelfSizeBinding {
		return SheetShelfSizeBinding.inflate(inflater, container, false)
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
		val slider = binding.sliderGrid
		when (v.id) {
			R.id.button_small -> slider.setValueRounded(slider.value - slider.stepSize)
			R.id.button_large -> slider.setValueRounded(slider.value + slider.stepSize)
		}
	}

	companion object {

		private const val TAG = "ShelfSizeBottomSheet"

		fun show(fm: FragmentManager) = ShelfSizeBottomSheet().show(fm, TAG)
	}
}
