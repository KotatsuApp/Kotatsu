package org.koitharu.kotatsu.settings.userdata

class StorageUsage(
	val savedManga: Item,
	val pagesCache: Item,
	val otherCache: Item,
	val available: Item,
) {

	class Item(
		val bytes: Long,
		val percent: Float,
	) {

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false

			other as Item

			if (bytes != other.bytes) return false
			return percent == other.percent
		}

		override fun hashCode(): Int {
			var result = bytes.hashCode()
			result = 31 * result + percent.hashCode()
			return result
		}
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as StorageUsage

		if (savedManga != other.savedManga) return false
		if (pagesCache != other.pagesCache) return false
		if (otherCache != other.otherCache) return false
		return available == other.available
	}

	override fun hashCode(): Int {
		var result = savedManga.hashCode()
		result = 31 * result + pagesCache.hashCode()
		result = 31 * result + otherCache.hashCode()
		result = 31 * result + available.hashCode()
		return result
	}
}
