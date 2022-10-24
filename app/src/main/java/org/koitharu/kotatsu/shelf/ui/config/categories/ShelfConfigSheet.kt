package org.koitharu.kotatsu.shelf.ui.config.categories

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseBottomSheet
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.databinding.SheetBaseBinding

@AndroidEntryPoint
class ShelfConfigSheet :
	BaseBottomSheet<SheetBaseBinding>(),
	OnListItemClickListener<ShelfConfigModel>,
	View.OnClickListener {

	private val viewModel by viewModels<ShelfConfigViewModel>()

	override fun onInflateView(inflater: LayoutInflater, container: ViewGroup?): SheetBaseBinding {
		return SheetBaseBinding.inflate(inflater, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		binding.headerBar.setTitle(R.string.settings)
		binding.buttonDone.isVisible = true
		binding.buttonDone.setOnClickListener(this)
		val adapter = ShelfConfigAdapter(this)
		binding.recyclerView.adapter = adapter

		viewModel.content.observe(viewLifecycleOwner) { adapter.items = it }
	}

	override fun onItemClick(item: ShelfConfigModel, view: View) {
		viewModel.toggleItem(item)
	}

	override fun onClick(v: View?) {
		dismiss()
	}

	companion object {

		private const val TAG = "ShelfCategoriesConfigSheet"

		fun show(fm: FragmentManager) = ShelfConfigSheet().show(fm, TAG)
	}
}
