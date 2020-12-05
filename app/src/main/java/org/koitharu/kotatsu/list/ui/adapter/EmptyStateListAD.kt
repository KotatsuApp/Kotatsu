package org.koitharu.kotatsu.list.ui.adapter

import android.widget.TextView
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegate
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.list.ui.model.EmptyState
import org.koitharu.kotatsu.list.ui.model.ListModel

fun emptyStateListAD() = adapterDelegate<EmptyState, ListModel>(R.layout.item_empty_state) {

	bind {
		(itemView as TextView).setText(item.text)
	}
}