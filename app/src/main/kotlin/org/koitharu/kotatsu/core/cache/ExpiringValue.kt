package org.koitharu.kotatsu.core.cache

import android.os.SystemClock
import java.util.concurrent.TimeUnit

class ExpiringValue<T>(
	private val value: T,
	lifetime: Long,
	timeUnit: TimeUnit,
) {

	private val expiresAt = SystemClock.elapsedRealtime() + timeUnit.toMillis(lifetime)

	val isExpired: Boolean
		get() = SystemClock.elapsedRealtime() >= expiresAt

	fun get(): T? = if (isExpired) null else value

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as ExpiringValue<*>

		if (value != other.value) return false
		return expiresAt == other.expiresAt
	}

	override fun hashCode(): Int {
		var result = value?.hashCode() ?: 0
		result = 31 * result + expiresAt.hashCode()
		return result
	}
}
