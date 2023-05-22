package org.koitharu.kotatsu.core.exceptions

import org.koitharu.kotatsu.parsers.util.mapNotNullToSet

class CompositeException(val errors: Collection<Throwable>) : Exception() {

	override val message: String = errors.mapNotNullToSet { it.message }.joinToString()
}