package org.koitharu.kotatsu.settings.tracker.categories

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.core.ui.BaseBottomSheet
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.databinding.SheetBaseBinding

@AndroidEntryPoint
class TrackerCategoriesConfigSheet :
	BaseBottomSheet<SheetBaseBinding>(),
	OnListItemClickListener<FavouriteCategory>,
	View.OnClickListener {

	private val viewModel by viewModels<TrackerCategoriesConfigViewModel>()

	override fun onCreateViewBinding(inflater: LayoutInflater, container: ViewGroup?): SheetBaseBinding {
		return SheetBaseBinding.inflate(inflater, container, false)
	}

	override fun onViewBindingCreated(binding: SheetBaseBinding, savedInstanceState: Bundle?) {
		super.onViewBindingCreated(binding, savedInstanceState)
		binding.headerBar.setTitle(R.string.favourites_categories)
		binding.buttonDone.isVisible = true
		binding.buttonDone.setOnClickListener(this)
		val adapter = TrackerCategoriesConfigAdapter(this)
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

		private const val TAG = "TrackerCategoriesConfigSheet"

		fun show(fm: FragmentManager) = TrackerCategoriesConfigSheet().show(fm, TAG)
	}
}
