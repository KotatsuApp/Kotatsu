package org.koitharu.kotatsu.core.util

class CompositeResult private constructor(
	private var successCount: Int,
	private val errors: List<Throwable>,
) {

	val size: Int
		get() = successCount + errors.size

	val failures: List<Throwable>
		get() = errors

	val isEmpty: Boolean
		get() = errors.isEmpty() && successCount == 0

	val isAllSuccess: Boolean
		get() = errors.isEmpty()

	val isAllFailed: Boolean
		get() = successCount == 0 && errors.isNotEmpty()

	operator fun plus(result: Result<*>): CompositeResult = CompositeResult(
		successCount = successCount + if (result.isSuccess) 1 else 0,
		errors = errors + listOfNotNull(result.exceptionOrNull()),
	)

	operator fun plus(other: CompositeResult): CompositeResult = CompositeResult(
		successCount = successCount + other.successCount,
		errors = errors + other.errors,
	)

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as CompositeResult

		if (successCount != other.successCount) return false
		if (errors != other.errors) return false

		return true
	}

	override fun hashCode(): Int {
		var result = successCount
		result = 31 * result + errors.hashCode()
		return result
	}

	companion object {

		val EMPTY = CompositeResult(0, emptyList())

		fun success() = CompositeResult(1, emptyList())

		fun failure(error: Throwable) = CompositeResult(0, listOf(error))
	}
}
