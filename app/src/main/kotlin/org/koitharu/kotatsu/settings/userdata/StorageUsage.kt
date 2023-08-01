package org.koitharu.kotatsu.settings.userdata

data class StorageUsage(
	val savedManga: Item,
	val pagesCache: Item,
	val otherCache: Item,
	val available: Item,
) {
	data class Item(
		val bytes: Long,
		val percent: Float,
	)
}
