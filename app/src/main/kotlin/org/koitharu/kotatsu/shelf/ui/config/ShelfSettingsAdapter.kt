package org.koitharu.kotatsu.shelf.ui.config

import org.koitharu.kotatsu.core.ui.BaseListAdapter
import org.koitharu.kotatsu.list.ui.model.ListModel

class ShelfSettingsAdapter(
	listener: ShelfSettingsListener,
) : BaseListAdapter<ListModel>() {

	init {
		delegatesManager.addDelegate(shelfCategoryAD(listener))
			.addDelegate(shelfSectionAD(listener))
	}
}
