package org.koitharu.kotatsu.core.backup

class CompositeResult {

	private var successCount: Int = 0
	private val errors = ArrayList<Throwable?>()

	val size: Int
		get() = successCount + errors.size

	val failures: List<Throwable>
		get() = errors.filterNotNull()

	val isEmpty: Boolean
		get() = errors.isEmpty() && successCount == 0

	val isAllSuccess: Boolean
		get() = errors.none { it != null }

	val isAllFailed: Boolean
		get() = successCount == 0 && errors.isNotEmpty()

	operator fun plusAssign(result: Result<*>) {
		when {
			result.isSuccess -> successCount++
			result.isFailure -> errors.add(result.exceptionOrNull())
		}
	}

	operator fun plusAssign(other: CompositeResult) {
		this.successCount += other.successCount
		this.errors += other.errors
	}

	operator fun plus(other: CompositeResult): CompositeResult {
		val result = CompositeResult()
		result.successCount = this.successCount + other.successCount
		result.errors.addAll(this.errors)
		result.errors.addAll(other.errors)
		return result
	}
}
