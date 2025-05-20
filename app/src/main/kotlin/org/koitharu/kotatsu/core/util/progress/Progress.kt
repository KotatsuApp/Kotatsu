package org.koitharu.kotatsu.core.util.progress

data class Progress(
	val progress: Int,
	val total: Int,
) : Comparable<Progress> {

	val percent: Float
		get() = if (total == 0) 0f else progress / total.toFloat()

	val isEmpty: Boolean
		get() = progress == 0

	val isFull: Boolean
		get() = progress == total

	val isIndeterminate: Boolean
		get() = total < 0

	override fun compareTo(other: Progress): Int = if (total == other.total) {
		progress.compareTo(other.progress)
	} else {
		percent.compareTo(other.percent)
	}

	operator fun inc() = if (isFull) {
		this
	} else {
		copy(
			progress = progress + 1,
			total = total,
		)
	}

	operator fun dec() = if (isEmpty) {
		this
	} else {
		copy(
			progress = progress - 1,
			total = total,
		)
	}

	operator fun plus(child: Progress) = Progress(
		progress = progress * child.total + child.progress,
		total = total * child.total,
	)

	fun percentSting() = (percent * 100f).toInt().toString()

	companion object {

		val INDETERMINATE = Progress(0, -1)
	}
}
