package org.koitharu.kotatsu.list.ui.adapter

import org.koitharu.kotatsu.list.domain.ListFilterOption

interface QuickFilterClickListener {

	fun onFilterOptionClick(option: ListFilterOption)
}
