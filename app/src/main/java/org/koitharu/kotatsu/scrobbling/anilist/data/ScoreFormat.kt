package org.koitharu.kotatsu.scrobbling.anilist.data

import org.koitharu.kotatsu.utils.ext.printStackTraceDebug

enum class ScoreFormat {

	POINT_100, POINT_10_DECIMAL, POINT_10, POINT_5, POINT_3;

	fun normalize(score: Float): Float = when (this) {
		POINT_100 -> score / 100f
		POINT_10_DECIMAL,
		POINT_10 -> score / 10f

		POINT_5 -> score / 5f
		POINT_3 -> score / 3f
	}

	companion object {

		fun of(rawValue: String?): ScoreFormat {
			rawValue ?: return POINT_10_DECIMAL
			return runCatching { valueOf(rawValue) }
				.onFailure { it.printStackTraceDebug() }
				.getOrDefault(POINT_10_DECIMAL)
		}
	}
}
