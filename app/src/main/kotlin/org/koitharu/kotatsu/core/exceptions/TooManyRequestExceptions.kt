package org.koitharu.kotatsu.core.exceptions

import okio.IOException
import java.util.Date

class TooManyRequestExceptions(
	val url: String,
	val retryAt: Date?,
) : IOException() {

	val retryAfter: Long
		get() = if (retryAt == null) 0 else (retryAt.time - System.currentTimeMillis()).coerceAtLeast(0)
}
