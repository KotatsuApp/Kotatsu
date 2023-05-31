package org.koitharu.kotatsu.list.ui.filter

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
import org.koitharu.kotatsu.core.ui.util.CollapseActionViewCallback
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.parentFragmentViewModels
import org.koitharu.kotatsu.databinding.SheetFilterBinding
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.remotelist.ui.RemoteListViewModel

class FilterSheetFragment :
	BaseAdaptiveSheet<SheetFilterBinding>(),
	AdaptiveSheetCallback,
	AsyncListDiffer.ListListener<ListModel> {

	private val viewModel by parentFragmentViewModels<RemoteListViewModel>()
	private var collapsibleActionViewCallback: CollapseActionViewCallback? = null

	override fun onCreateViewBinding(inflater: LayoutInflater, container: ViewGroup?): SheetFilterBinding {
		return SheetFilterBinding.inflate(inflater, container, false)
	}

	override fun onViewBindingCreated(binding: SheetFilterBinding, savedInstanceState: Bundle?) {
		super.onViewBindingCreated(binding, savedInstanceState)
		addSheetCallback(this)
		val adapter = FilterAdapter(viewModel, this)
		binding.recyclerView.adapter = adapter
		viewModel.filterItems.observe(viewLifecycleOwner, adapter::setItems)
	}

	override fun onDestroyView() {
		super.onDestroyView()
		collapsibleActionViewCallback = null
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

		fun show(fm: FragmentManager) = FilterSheetFragment().show(fm, TAG)
	}
}
