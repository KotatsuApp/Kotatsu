package org.koitharu.kotatsu.list.ui.filter

import com.hannesdorfmann.adapterdelegates4.AsyncListDifferDelegationAdapter

class FilterAdapter(
	listener: OnFilterChangedListener,
) : AsyncListDifferDelegationAdapter<FilterItem>(
	FilterDiffCallback(),
	filterSortDelegate(listener),
	filterTagDelegate(listener),
	filterHeaderDelegate(),
	filterLoadingDelegate(),
)