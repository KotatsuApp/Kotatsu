package org.koitharu.kotatsu.list.ui.adapter

import android.view.View
import org.koitharu.kotatsu.core.ui.widgets.TipView

interface MangaListListener : MangaDetailsClickListener, ListStateHolderListener, ListHeaderClickListener,
	TipView.OnButtonClickListener, QuickFilterClickListener {

	fun onFilterClick(view: View?)
}
