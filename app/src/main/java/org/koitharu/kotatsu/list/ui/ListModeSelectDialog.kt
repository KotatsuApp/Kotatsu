package org.koitharu.kotatsu.list.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import org.koin.android.ext.android.inject
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.AlertDialogFragment
import org.koitharu.kotatsu.base.ui.widgets.CheckableButtonGroup
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.ListMode
import org.koitharu.kotatsu.databinding.DialogListModeBinding
import org.koitharu.kotatsu.utils.ext.setValueRounded
import org.koitharu.kotatsu.utils.progress.IntPercentLabelFormatter

class ListModeSelectDialog : AlertDialogFragment<DialogListModeBinding>(),
	CheckableButtonGroup.OnCheckedChangeListener, Slider.OnSliderTouchListener {

	private val settings by inject<AppSettings>(mode = LazyThreadSafetyMode.NONE)

	override fun onInflateView(
		inflater: LayoutInflater,
		container: ViewGroup?
	) = DialogListModeBinding.inflate(inflater, container, false)

	override fun onBuildDialog(builder: MaterialAlertDialogBuilder) {
		builder.setTitle(R.string.list_mode)
			.setPositiveButton(R.string.done, null)
			.setCancelable(true)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		val mode = settings.listMode
		binding.buttonList.isChecked = mode == ListMode.LIST
		binding.buttonListDetailed.isChecked = mode == ListMode.DETAILED_LIST
		binding.buttonGrid.isChecked = mode == ListMode.GRID
		binding.textViewGridTitle.isVisible = mode == ListMode.GRID
		binding.sliderGrid.isVisible = mode == ListMode.GRID

		binding.sliderGrid.setLabelFormatter(IntPercentLabelFormatter())
		binding.sliderGrid.setValueRounded(settings.gridSize.toFloat())
		binding.sliderGrid.addOnSliderTouchListener(this)

		binding.checkableGroup.onCheckedChangeListener = this
	}

	override fun onCheckedChanged(group: CheckableButtonGroup, checkedId: Int) {
		val mode = when (checkedId) {
			R.id.button_list -> ListMode.LIST
			R.id.button_list_detailed -> ListMode.DETAILED_LIST
			R.id.button_grid -> ListMode.GRID
			else -> return
		}
		binding.textViewGridTitle.isVisible = mode == ListMode.GRID
		binding.sliderGrid.isVisible = mode == ListMode.GRID
		settings.listMode = mode
	}

	override fun onStartTrackingTouch(slider: Slider) = Unit

	override fun onStopTrackingTouch(slider: Slider) {
		settings.gridSize = slider.value.toInt()
	}

	companion object {

		private const val TAG = "ListModeSelectDialog"

		fun show(fm: FragmentManager) = ListModeSelectDialog().show(fm, TAG)
	}
}