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
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.ui.BaseBottomSheet
import org.koitharu.kotatsu.core.util.ext.setValueRounded
import org.koitharu.kotatsu.core.util.progress.IntPercentLabelFormatter
import org.koitharu.kotatsu.databinding.SheetShelfSizeBinding
import javax.inject.Inject

@AndroidEntryPoint
class ShelfSizeBottomSheet :
	BaseBottomSheet<SheetShelfSizeBinding>(),
	Slider.OnChangeListener,
	View.OnClickListener {

	@Inject
	lateinit var settings: AppSettings
	private var labelFormatter: LabelFormatter? = null

	override fun onCreateViewBinding(inflater: LayoutInflater, container: ViewGroup?): SheetShelfSizeBinding {
		return SheetShelfSizeBinding.inflate(inflater, container, false)
	}

	override fun onViewBindingCreated(binding: SheetShelfSizeBinding, savedInstanceState: Bundle?) {
		super.onViewBindingCreated(binding, savedInstanceState)
		labelFormatter = IntPercentLabelFormatter(binding.root.context)
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
		requireViewBinding().textViewLabel.text = labelFormatter?.getFormattedValue(value)
	}

	override fun onClick(v: View) {
		val slider = requireViewBinding().sliderGrid
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
