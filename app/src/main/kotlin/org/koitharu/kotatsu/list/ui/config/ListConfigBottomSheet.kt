package org.koitharu.kotatsu.list.ui.config

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CompoundButton
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.slider.Slider
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.prefs.ListMode
import org.koitharu.kotatsu.core.ui.sheet.BaseAdaptiveSheet
import org.koitharu.kotatsu.core.util.ext.setValueRounded
import org.koitharu.kotatsu.core.util.ext.showDistinct
import org.koitharu.kotatsu.core.util.ext.withArgs
import org.koitharu.kotatsu.core.util.progress.IntPercentLabelFormatter
import org.koitharu.kotatsu.databinding.SheetListModeBinding

@AndroidEntryPoint
class ListConfigBottomSheet :
	BaseAdaptiveSheet<SheetListModeBinding>(),
	Slider.OnChangeListener,
	MaterialButtonToggleGroup.OnButtonCheckedListener, CompoundButton.OnCheckedChangeListener,
	AdapterView.OnItemSelectedListener {

	private val viewModel by viewModels<ListConfigViewModel>()

	override fun onCreateViewBinding(
		inflater: LayoutInflater,
		container: ViewGroup?,
	) = SheetListModeBinding.inflate(inflater, container, false)

	override fun onViewBindingCreated(binding: SheetListModeBinding, savedInstanceState: Bundle?) {
		super.onViewBindingCreated(binding, savedInstanceState)
		val mode = viewModel.listMode
		binding.buttonList.isChecked = mode == ListMode.LIST
		binding.buttonListDetailed.isChecked = mode == ListMode.DETAILED_LIST
		binding.buttonGrid.isChecked = mode == ListMode.GRID
		binding.textViewGridTitle.isVisible = mode == ListMode.GRID
		binding.sliderGrid.isVisible = mode == ListMode.GRID

		binding.sliderGrid.setLabelFormatter(IntPercentLabelFormatter(binding.root.context))
		binding.sliderGrid.setValueRounded(viewModel.gridSize.toFloat())
		binding.sliderGrid.addOnChangeListener(this)

		binding.checkableGroup.addOnButtonCheckedListener(this)

		binding.switchGrouping.isVisible = viewModel.isGroupingSupported
		if (viewModel.isGroupingSupported) {
			binding.switchGrouping.isEnabled = viewModel.isGroupingAvailable
		}
		binding.switchGrouping.isChecked = viewModel.isGroupingEnabled
		binding.switchGrouping.setOnCheckedChangeListener(this)

		val sortOrders = viewModel.getSortOrders()
		if (sortOrders != null) {
			binding.textViewOrderTitle.isVisible = true
			binding.spinnerOrder.adapter = ArrayAdapter(
				binding.spinnerOrder.context,
				android.R.layout.simple_spinner_dropdown_item,
				android.R.id.text1,
				sortOrders.map { binding.spinnerOrder.context.getString(it.titleResId) },
			)
			val selected = sortOrders.indexOf(viewModel.getSelectedSortOrder())
			if (selected >= 0) {
				binding.spinnerOrder.setSelection(selected, false)
			}
			binding.spinnerOrder.onItemSelectedListener = this
			binding.cardOrder.isVisible = true
		}
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
		viewModel.listMode = mode
	}

	override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
		when (buttonView.id) {
			R.id.switch_grouping -> viewModel.isGroupingEnabled = isChecked
		}
	}

	override fun onValueChange(slider: Slider, value: Float, fromUser: Boolean) {
		if (fromUser) {
			viewModel.gridSize = value.toInt()
		}
	}

	override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
		when (parent.id) {
			R.id.spinner_order -> {
				viewModel.setSortOrder(position)
				viewBinding?.switchGrouping?.isEnabled = viewModel.isGroupingAvailable
			}
		}
	}

	override fun onNothingSelected(parent: AdapterView<*>?) = Unit

	companion object {

		private const val TAG = "ListModeSelectDialog"
		const val ARG_SECTION = "section"

		fun show(fm: FragmentManager, section: ListConfigSection) = ListConfigBottomSheet().withArgs(1) {
			putParcelable(ARG_SECTION, section)
		}.showDistinct(fm, TAG)
	}
}
