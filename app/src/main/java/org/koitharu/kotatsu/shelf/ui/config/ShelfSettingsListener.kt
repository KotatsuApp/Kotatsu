package org.koitharu.kotatsu.shelf.ui.config

import androidx.recyclerview.widget.RecyclerView

interface ShelfSettingsListener {

	fun onItemCheckedChanged(item: ShelfSettingsItemModel, isChecked: Boolean)

	fun onDragHandleTouch(holder: RecyclerView.ViewHolder)
}
