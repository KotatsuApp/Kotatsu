package org.koitharu.kotatsu.core.exceptions

class CaughtException(cause: Throwable, override val message: String?) : RuntimeException(cause)