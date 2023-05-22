package org.koitharu.kotatsu.list.ui.filter

import androidx.recyclerview.widget.AsyncListDiffer.ListListener
import com.hannesdorfmann.adapterdelegates4.AsyncListDifferDelegationAdapter

class FilterAdapter(
	listener: OnFilterChangedListener,
	listListener: ListListener<FilterItem>,
) : AsyncListDifferDelegationAdapter<FilterItem>(
	FilterDiffCallback(),
	filterSortDelegate(listener),
	filterTagDelegate(listener),
	filterHeaderDelegate(),
	filterLoadingDelegate(),
	filterErrorDelegate(),
) {

	init {
		differ.addListListener(listListener)
	}
}
