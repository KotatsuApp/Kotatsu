package org.koitharu.kotatsu.details.ui.adapter

import com.hannesdorfmann.adapterdelegates4.ListDelegationAdapter
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.details.ui.model.MangaBranch

class BranchesAdapter(
	list: List<MangaBranch>,
	listener: OnListItemClickListener<MangaBranch>,
) : ListDelegationAdapter<List<MangaBranch>>() {

	init {
		delegatesManager.addDelegate(branchAD(listener))
		items = list
	}
}
