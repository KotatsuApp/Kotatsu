package org.koitharu.kotatsu.list.domain

interface QuickFilterListener {

	fun toggleFilterOption(option: ListFilterOption)

	fun clearFilter()
}
