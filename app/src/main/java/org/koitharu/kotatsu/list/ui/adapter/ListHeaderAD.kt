package org.koitharu.kotatsu.list.ui.adapter

import android.widget.TextView
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegate
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.list.ui.model.ListHeader
import org.koitharu.kotatsu.list.ui.model.ListModel

fun listHeaderAD() = adapterDelegate<ListHeader, ListModel>(R.layout.item_header) {

	bind {
		(itemView as TextView).text = item.text
	}
}