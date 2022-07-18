package org.koitharu.kotatsu.library.ui.adapter

import android.view.View
import org.koitharu.kotatsu.library.ui.model.LibrarySectionModel
import org.koitharu.kotatsu.list.ui.adapter.ListStateHolderListener
import org.koitharu.kotatsu.parsers.model.Manga

interface LibraryListEventListener : ListStateHolderListener {

	fun onItemClick(item: Manga, section: LibrarySectionModel, view: View)

	fun onItemLongClick(item: Manga, section: LibrarySectionModel, view: View): Boolean

	fun onSectionClick(section: LibrarySectionModel, view: View)
}