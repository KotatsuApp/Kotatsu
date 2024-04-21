package org.koitharu.kotatsu.core.util

import kotlinx.coroutines.flow.FlowCollector

class Event<T>(
	private val data: T,
) {
	private var isConsumed = false

	suspend fun consume(collector: FlowCollector<T>) {
		if (!isConsumed) {
			isConsumed = true
			collector.emit(data)
		}
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as Event<*>

		if (data != other.data) return false
		return isConsumed == other.isConsumed
	}

	override fun hashCode(): Int {
		var result = data?.hashCode() ?: 0
		result = 31 * result + isConsumed.hashCode()
		return result
	}

	override fun toString(): String {
		return "Event(data=$data, isConsumed=$isConsumed)"
	}
}
