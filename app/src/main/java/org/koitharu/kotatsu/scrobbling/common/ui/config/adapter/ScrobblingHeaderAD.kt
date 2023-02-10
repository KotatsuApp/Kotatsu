package org.koitharu.kotatsu.scrobbling.common.ui.config.adapter

import android.widget.TextView
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegate
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.scrobbling.common.domain.model.ScrobblingStatus

fun scrobblingHeaderAD() = adapterDelegate<ScrobblingStatus, ListModel>(R.layout.item_header) {

	bind {
		(itemView as TextView).text = context.resources
			.getStringArray(R.array.scrobbling_statuses)
			.getOrNull(item.ordinal)
	}
}
