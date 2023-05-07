package org.koitharu.kotatsu.sync.domain

class SyncAuthResult(
	val host: String,
	val email: String,
	val password: String,
	val token: String,
) {

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as SyncAuthResult

		if (host != other.host) return false
		if (email != other.email) return false
		if (password != other.password) return false
		return token == other.token
	}

	override fun hashCode(): Int {
		var result = host.hashCode()
		result = 31 * result + email.hashCode()
		result = 31 * result + password.hashCode()
		result = 31 * result + token.hashCode()
		return result
	}
}
