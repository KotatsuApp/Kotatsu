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
import org.koitharu.kotatsu.databinding.SheetListModeBinding
import org.koitharu.kotatsu.favourites.ui.list.FavouritesListFragment
import org.koitharu.kotatsu.history.domain.model.HistoryOrder
import org.koitharu.kotatsu.history.ui.HistoryListFragment
import javax.inject.Inject

@AndroidEntryPoint
class ListConfigBottomSheet :
	BaseAdaptiveSheet<SheetListModeBinding>(),
	Slider.OnChangeListener,
	MaterialButtonToggleGroup.OnButtonCheckedListener, CompoundButton.OnCheckedChangeListener,
	AdapterView.OnItemSelectedListener {

	@Inject
	lateinit var settings: AppSettings

	override fun onCreateViewBinding(
		inflater: LayoutInflater,
		container: ViewGroup?,
	) = SheetListModeBinding.inflate(inflater, container, false)

	override fun onViewBindingCreated(binding: SheetListModeBinding, savedInstanceState: Bundle?) {
		super.onViewBindingCreated(binding, savedInstanceState)
		val section = getSection()
		val mode = when (section) {
			Section.GENERAL -> settings.listMode
			Section.HISTORY -> settings.historyListMode
			Section.FAVORITES -> settings.favoritesListMode
		}
		binding.buttonList.isChecked = mode == ListMode.LIST
		binding.buttonListDetailed.isChecked = mode == ListMode.DETAILED_LIST
		binding.buttonGrid.isChecked = mode == ListMode.GRID
		binding.textViewGridTitle.isVisible = mode == ListMode.GRID
		binding.sliderGrid.isVisible = mode == ListMode.GRID

		binding.sliderGrid.setLabelFormatter(IntPercentLabelFormatter(binding.root.context))
		binding.sliderGrid.setValueRounded(settings.gridSize.toFloat())
		binding.sliderGrid.addOnChangeListener(this)

		binding.checkableGroup.addOnButtonCheckedListener(this)

		binding.switchGrouping.isVisible = section == Section.HISTORY
		if (section == Section.HISTORY) {
			binding.switchGrouping.isEnabled = settings.historySortOrder.isGroupingSupported()
		}
		binding.switchGrouping.isChecked = settings.isHistoryGroupingEnabled
		binding.switchGrouping.setOnCheckedChangeListener(this)

		if (section == Section.HISTORY) {
			binding.textViewOrderTitle.isVisible = true
			binding.spinnerOrder.adapter = ArrayAdapter(
				binding.spinnerOrder.context,
				android.R.layout.simple_spinner_dropdown_item,
				android.R.id.text1,
				HistoryOrder.entries.map { getString(it.titleResId) },
			)
			binding.spinnerOrder.setSelection(settings.historySortOrder.ordinal, false)
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
		when (getSection()) {
			Section.GENERAL -> settings.listMode = mode
			Section.HISTORY -> settings.historyListMode = mode
			Section.FAVORITES -> settings.favoritesListMode = mode
		}
	}

	override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
		when (buttonView.id) {
			R.id.switch_grouping -> settings.isHistoryGroupingEnabled = isChecked
		}
	}

	override fun onValueChange(slider: Slider, value: Float, fromUser: Boolean) {
		if (fromUser) {
			settings.gridSize = value.toInt()
		}
	}

	override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
		when (parent.id) {
			R.id.spinner_order -> {
				val value = HistoryOrder.entries[position]
				settings.historySortOrder = value
				viewBinding?.switchGrouping?.isEnabled = value.isGroupingSupported()
			}
		}
	}

	override fun onNothingSelected(parent: AdapterView<*>?) = Unit

	private fun getSection(): Section = when (parentFragment) {
		is HistoryListFragment -> Section.HISTORY
		is FavouritesListFragment -> Section.FAVORITES
		else -> Section.GENERAL
	}

	enum class Section {
		GENERAL, HISTORY, FAVORITES;
	}

	companion object {

		private const val TAG = "ListModeSelectDialog"

		fun show(fm: FragmentManager) = ListConfigBottomSheet().showDistinct(fm, TAG)
	}
}
