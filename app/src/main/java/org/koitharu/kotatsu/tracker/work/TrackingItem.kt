package org.koitharu.kotatsu.tracker.work

import org.koitharu.kotatsu.core.model.MangaTracking

class TrackingItem(
	val tracking: MangaTracking,
	val channelId: String?,
) {

	operator fun component1() = tracking

	operator fun component2() = channelId

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as TrackingItem

		if (tracking != other.tracking) return false
		if (channelId != other.channelId) return false

		return true
	}

	override fun hashCode(): Int {
		var result = tracking.hashCode()
		result = 31 * result + channelId.hashCode()
		return result
	}
}