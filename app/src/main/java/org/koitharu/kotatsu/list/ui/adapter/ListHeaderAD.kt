package org.koitharu.kotatsu.list.ui.adapter

import android.widget.TextView
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegate
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.list.ui.model.ListHeader
import org.koitharu.kotatsu.list.ui.model.ListModel

fun listHeaderAD() = adapterDelegate<ListHeader, ListModel>(R.layout.item_header) {

	bind {
		val textView = (itemView as TextView)
		if (item.text != null) {
			textView.text = item.text
		} else {
			textView.setText(item.textRes)
		}
	}
}