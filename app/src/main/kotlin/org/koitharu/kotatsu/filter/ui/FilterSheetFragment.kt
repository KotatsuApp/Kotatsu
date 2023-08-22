package org.koitharu.kotatsu.filter.ui

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.LinearLayoutManager
import org.koitharu.kotatsu.core.ui.sheet.AdaptiveSheetBehavior
import org.koitharu.kotatsu.core.ui.sheet.AdaptiveSheetCallback
import org.koitharu.kotatsu.core.ui.sheet.BaseAdaptiveSheet
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.showDistinct
import org.koitharu.kotatsu.databinding.SheetFilterBinding
import org.koitharu.kotatsu.list.ui.adapter.TypedListSpacingDecoration
import org.koitharu.kotatsu.list.ui.model.ListModel

class FilterSheetFragment :
	BaseAdaptiveSheet<SheetFilterBinding>(),
	AdaptiveSheetCallback,
	AsyncListDiffer.ListListener<ListModel> {

	override fun onCreateViewBinding(inflater: LayoutInflater, container: ViewGroup?): SheetFilterBinding {
		return SheetFilterBinding.inflate(inflater, container, false)
	}

	override fun onViewBindingCreated(binding: SheetFilterBinding, savedInstanceState: Bundle?) {
		super.onViewBindingCreated(binding, savedInstanceState)
		val filter = (requireActivity() as FilterOwner).filter
		addSheetCallback(this)
		val adapter = FilterAdapter(filter, this)
		binding.recyclerView.adapter = adapter
		filter.filterItems.observe(viewLifecycleOwner, adapter)
		binding.recyclerView.addItemDecoration(TypedListSpacingDecoration(binding.root.context, true))

		if (dialog == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			binding.recyclerView.scrollIndicators = 0
		}
	}

	override fun onCurrentListChanged(previousList: MutableList<ListModel>, currentList: MutableList<ListModel>) {
		if (currentList.size > previousList.size && view != null) {
			(requireViewBinding().recyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(0, 0)
		}
	}

	override fun onStateChanged(sheet: View, newState: Int) {
		viewBinding?.recyclerView?.isFastScrollerEnabled = newState == AdaptiveSheetBehavior.STATE_EXPANDED
	}

	companion object {

		private const val TAG = "FilterBottomSheet"

		fun show(fm: FragmentManager) = FilterSheetFragment().showDistinct(fm, TAG)
	}
}
