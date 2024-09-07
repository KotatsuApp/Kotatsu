package org.koitharu.kotatsu.core.ui.list

import androidx.recyclerview.widget.RecyclerView

abstract class BaseListSelectionCallback(
	protected val recyclerView: RecyclerView,
) : ListSelectionController.Callback {

	override fun onSelectionChanged(controller: ListSelectionController, count: Int) {
		recyclerView.invalidateItemDecorations()
	}
}
