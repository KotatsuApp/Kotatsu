package org.koitharu.kotatsu.list.ui.adapter

import android.widget.TextView
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegate
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.model.DateTimeAgo
import org.koitharu.kotatsu.list.ui.model.ListModel

fun relatedDateItemAD() = adapterDelegate<DateTimeAgo, ListModel>(R.layout.item_header) {

	bind {
		(itemView as TextView).text = item.format(context.resources)
	}
}
