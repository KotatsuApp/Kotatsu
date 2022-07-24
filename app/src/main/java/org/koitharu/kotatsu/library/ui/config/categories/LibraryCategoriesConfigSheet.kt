package org.koitharu.kotatsu.library.ui.config.categories

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseBottomSheet
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.databinding.SheetBaseBinding
import org.koitharu.kotatsu.utils.BottomSheetToolbarController

class LibraryCategoriesConfigSheet :
	BaseBottomSheet<SheetBaseBinding>(),
	OnListItemClickListener<FavouriteCategory>,
	View.OnClickListener {

	private val viewModel by viewModel<LibraryCategoriesConfigViewModel>()

	override fun onInflateView(inflater: LayoutInflater, container: ViewGroup?): SheetBaseBinding {
		return SheetBaseBinding.inflate(inflater, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		binding.toolbar.setNavigationOnClickListener { dismiss() }
		binding.toolbar.setTitle(R.string.favourites_categories)
		binding.buttonDone.isVisible = true
		binding.buttonDone.setOnClickListener(this)
		behavior?.addBottomSheetCallback(BottomSheetToolbarController(binding.toolbar))
		if (!resources.getBoolean(R.bool.is_tablet)) {
			binding.toolbar.navigationIcon = null
		}
		val adapter = LibraryCategoriesConfigAdapter(this)
		binding.recyclerView.adapter = adapter

		viewModel.content.observe(viewLifecycleOwner) { adapter.items = it }
	}

	override fun onItemClick(item: FavouriteCategory, view: View) {
		viewModel.toggleItem(item)
	}

	override fun onClick(v: View?) {
		dismiss()
	}

	companion object {

		private const val TAG = "LibraryCategoriesConfigSheet"

		fun show(fm: FragmentManager) = LibraryCategoriesConfigSheet().show(fm, TAG)
	}
}
