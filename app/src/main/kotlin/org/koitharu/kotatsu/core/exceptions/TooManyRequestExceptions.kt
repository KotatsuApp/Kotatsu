package org.koitharu.kotatsu.core.exceptions

import okio.IOException
import java.util.Date

class TooManyRequestExceptions(
	val url: String,
	val retryAt: Date?,
) : IOException()
