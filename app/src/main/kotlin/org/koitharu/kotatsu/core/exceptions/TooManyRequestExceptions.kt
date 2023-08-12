package org.koitharu.kotatsu.core.exceptions

import okio.IOException
import java.time.Instant
import java.time.temporal.ChronoUnit

class TooManyRequestExceptions(
	val url: String,
	val retryAt: Instant?,
) : IOException() {
	val retryAfter: Long
		get() = retryAt?.until(Instant.now(), ChronoUnit.MILLIS) ?: 0
}
