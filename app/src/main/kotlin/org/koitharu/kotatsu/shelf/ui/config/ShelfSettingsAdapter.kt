package org.koitharu.kotatsu.shelf.ui.config

import com.hannesdorfmann.adapterdelegates4.AsyncListDifferDelegationAdapter
import org.koitharu.kotatsu.list.ui.ListModelDiffCallback
import org.koitharu.kotatsu.list.ui.model.ListModel

class ShelfSettingsAdapter(
	listener: ShelfSettingsListener,
) : AsyncListDifferDelegationAdapter<ListModel>(ListModelDiffCallback) {

	init {
		delegatesManager.addDelegate(shelfCategoryAD(listener))
			.addDelegate(shelfSectionAD(listener))
	}
}
