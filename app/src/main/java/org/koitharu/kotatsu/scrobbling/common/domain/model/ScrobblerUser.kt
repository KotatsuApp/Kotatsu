package org.koitharu.kotatsu.scrobbling.common.domain.model

class ScrobblerUser(
	val id: Long,
	val nickname: String,
	val avatar: String,
	val service: ScrobblerService,
) {

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as ScrobblerUser

		if (id != other.id) return false
		if (nickname != other.nickname) return false
		if (avatar != other.avatar) return false
		if (service != other.service) return false

		return true
	}

	override fun hashCode(): Int {
		var result = id.hashCode()
		result = 31 * result + nickname.hashCode()
		result = 31 * result + avatar.hashCode()
		result = 31 * result + service.hashCode()
		return result
	}
}
