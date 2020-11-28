package org.koitharu.kotatsu.list.ui.adapter

import android.widget.TextView
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegate
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.DateTimeAgo

fun relatedDateItemAD() = adapterDelegate<DateTimeAgo, Any>(R.layout.item_header) {

	bind {
		(itemView as TextView).text = item.format(context.resources)
	}
}