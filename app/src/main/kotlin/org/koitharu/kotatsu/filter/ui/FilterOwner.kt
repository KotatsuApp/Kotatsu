package org.koitharu.kotatsu.filter.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.StateFlow
import org.koitharu.kotatsu.core.util.ext.values
import org.koitharu.kotatsu.filter.ui.model.FilterHeaderModel
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.parsers.model.MangaTag

interface FilterOwner : OnFilterChangedListener {

	val filterItems: StateFlow<List<ListModel>>

	val header: StateFlow<FilterHeaderModel>

	fun applyFilter(tags: Set<MangaTag>)

	companion object {

		fun from(activity: FragmentActivity): FilterOwner {
			for (f in activity.supportFragmentManager.fragments) {
				return find(f) ?: continue
			}
			error("Cannot find FilterOwner")
		}

		fun find(fragment: Fragment): FilterOwner? {
			return fragment.viewModelStore.values.firstNotNullOfOrNull { it as? FilterOwner }
		}
	}
}
