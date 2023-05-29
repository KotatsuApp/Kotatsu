package org.koitharu.kotatsu.shelf.ui.adapter

import android.view.View
import org.koitharu.kotatsu.shelf.ui.model.ShelfSectionModel
import org.koitharu.kotatsu.list.ui.adapter.ListStateHolderListener
import org.koitharu.kotatsu.parsers.model.Manga

interface ShelfListEventListener : ListStateHolderListener {

	fun onItemClick(item: Manga, section: ShelfSectionModel, view: View)

	fun onItemLongClick(item: Manga, section: ShelfSectionModel, view: View): Boolean

	fun onSectionClick(section: ShelfSectionModel, view: View)
}
