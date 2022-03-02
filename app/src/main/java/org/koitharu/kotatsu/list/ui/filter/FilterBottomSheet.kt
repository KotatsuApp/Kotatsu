package org.koitharu.kotatsu.list.ui.filter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseBottomSheet
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.databinding.SheetFilterBinding
import org.koitharu.kotatsu.utils.ext.withArgs

class FilterBottomSheet : BaseBottomSheet<SheetFilterBinding>() {

	private val viewModel by viewModel<FilterViewModel> {
		parametersOf(
			requireArguments().getParcelable<MangaSource>(ARG_SOURCE),
			requireArguments().getParcelable<FilterState>(ARG_STATE),
		)
	}

	override fun onInflateView(inflater: LayoutInflater, container: ViewGroup?): SheetFilterBinding {
		return SheetFilterBinding.inflate(inflater, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		binding.toolbar.setNavigationOnClickListener { dismiss() }
		if (!resources.getBoolean(R.bool.is_tablet)) {
			binding.toolbar.navigationIcon = null
		}
		val adapter = FilterAdapter(viewModel)
		binding.recyclerView.adapter = adapter
		viewModel.filter.observe(viewLifecycleOwner, adapter::setItems)
		viewModel.result.observe(viewLifecycleOwner) {
			parentFragmentManager.setFragmentResult(REQUEST_KEY, bundleOf(ARG_STATE to it))
		}
	}

	override fun onCreateDialog(savedInstanceState: Bundle?) = super.onCreateDialog(savedInstanceState).also {
		val behavior = (it as? BottomSheetDialog)?.behavior ?: return@also
		behavior.addBottomSheetCallback(
			object : BottomSheetBehavior.BottomSheetCallback() {

				override fun onSlide(bottomSheet: View, slideOffset: Float) = Unit

				override fun onStateChanged(bottomSheet: View, newState: Int) {
					if (newState == BottomSheetBehavior.STATE_EXPANDED) {
						binding.toolbar.setNavigationIcon(R.drawable.ic_cross)
					} else {
						binding.toolbar.navigationIcon = null
					}
				}
			}
		)
	}

	companion object {

		const val REQUEST_KEY = "filter"

		const val ARG_STATE = "state"
		private const val TAG = "FilterBottomSheet"
		private const val ARG_SOURCE = "source"

		fun show(
			fm: FragmentManager,
			source: MangaSource,
			state: FilterState,
		) = FilterBottomSheet().withArgs(2) {
			putParcelable(ARG_SOURCE, source)
			putParcelable(ARG_STATE, state)
		}.show(fm, TAG)
	}
}