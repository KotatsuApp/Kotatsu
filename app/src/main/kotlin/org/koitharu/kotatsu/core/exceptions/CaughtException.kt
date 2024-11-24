package org.koitharu.kotatsu.core.exceptions

class CaughtException(cause: Throwable) : RuntimeException("${cause.javaClass.simpleName}(${cause.message})", cause)
