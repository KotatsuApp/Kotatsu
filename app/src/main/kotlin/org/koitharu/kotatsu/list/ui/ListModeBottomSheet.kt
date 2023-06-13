package org.koitharu.kotatsu.list.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.slider.Slider
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.ListMode
import org.koitharu.kotatsu.core.ui.sheet.BaseAdaptiveSheet
import org.koitharu.kotatsu.core.util.ext.setValueRounded
import org.koitharu.kotatsu.core.util.ext.showDistinct
import org.koitharu.kotatsu.core.util.progress.IntPercentLabelFormatter
import org.koitharu.kotatsu.databinding.DialogListModeBinding
import javax.inject.Inject

@AndroidEntryPoint
class ListModeBottomSheet :
	BaseAdaptiveSheet<DialogListModeBinding>(),
	Slider.OnChangeListener,
	MaterialButtonToggleGroup.OnButtonCheckedListener {

	@Inject
	lateinit var settings: AppSettings

	override fun onCreateViewBinding(
		inflater: LayoutInflater,
		container: ViewGroup?,
	) = DialogListModeBinding.inflate(inflater, container, false)

	override fun onViewBindingCreated(binding: DialogListModeBinding, savedInstanceState: Bundle?) {
		super.onViewBindingCreated(binding, savedInstanceState)
		val mode = settings.listMode
		binding.buttonList.isChecked = mode == ListMode.LIST
		binding.buttonListDetailed.isChecked = mode == ListMode.DETAILED_LIST
		binding.buttonGrid.isChecked = mode == ListMode.GRID
		binding.textViewGridTitle.isVisible = mode == ListMode.GRID
		binding.sliderGrid.isVisible = mode == ListMode.GRID

		binding.sliderGrid.setLabelFormatter(IntPercentLabelFormatter(binding.root.context))
		binding.sliderGrid.setValueRounded(settings.gridSize.toFloat())
		binding.sliderGrid.addOnChangeListener(this)

		binding.checkableGroup.addOnButtonCheckedListener(this)
	}

	override fun onButtonChecked(group: MaterialButtonToggleGroup?, checkedId: Int, isChecked: Boolean) {
		if (!isChecked) {
			return
		}
		val mode = when (checkedId) {
			R.id.button_list -> ListMode.LIST
			R.id.button_list_detailed -> ListMode.DETAILED_LIST
			R.id.button_grid -> ListMode.GRID
			else -> return
		}
		requireViewBinding().textViewGridTitle.isVisible = mode == ListMode.GRID
		requireViewBinding().sliderGrid.isVisible = mode == ListMode.GRID
		settings.listMode = mode
	}

	override fun onValueChange(slider: Slider, value: Float, fromUser: Boolean) {
		if (fromUser) {
			settings.gridSize = value.toInt()
		}
	}

	companion object {

		private const val TAG = "ListModeSelectDialog"

		fun show(fm: FragmentManager) = ListModeBottomSheet().showDistinct(fm, TAG)
	}
}
