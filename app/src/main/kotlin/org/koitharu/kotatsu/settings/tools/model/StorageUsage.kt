package org.koitharu.kotatsu.settings.tools.model

class StorageUsage(
	val savedManga: Item,
	val pagesCache: Item,
	val otherCache: Item,
	val available: Item,
) {

	class Item(
		val bytes: Long,
		val percent: Float,
	)
}