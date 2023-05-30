package org.koitharu.kotatsu.settings.tracker.categories

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.ui.sheet.BaseAdaptiveSheet
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.databinding.SheetBaseBinding

@AndroidEntryPoint
class TrackerCategoriesConfigSheet :
	BaseAdaptiveSheet<SheetBaseBinding>(),
	OnListItemClickListener<FavouriteCategory> {

	private val viewModel by viewModels<TrackerCategoriesConfigViewModel>()

	override fun onCreateViewBinding(inflater: LayoutInflater, container: ViewGroup?): SheetBaseBinding {
		return SheetBaseBinding.inflate(inflater, container, false)
	}

	override fun onViewBindingCreated(binding: SheetBaseBinding, savedInstanceState: Bundle?) {
		super.onViewBindingCreated(binding, savedInstanceState)
		binding.headerBar.setTitle(R.string.favourites_categories)
		val adapter = TrackerCategoriesConfigAdapter(this)
		binding.recyclerView.adapter = adapter

		viewModel.content.observe(viewLifecycleOwner) { adapter.items = it }
	}

	override fun onItemClick(item: FavouriteCategory, view: View) {
		viewModel.toggleItem(item)
	}

	companion object {

		private const val TAG = "TrackerCategoriesConfigSheet"

		fun show(fm: FragmentManager) = TrackerCategoriesConfigSheet().show(fm, TAG)
	}
}
