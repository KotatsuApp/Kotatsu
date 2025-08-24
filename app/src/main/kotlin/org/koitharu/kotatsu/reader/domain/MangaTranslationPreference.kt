package org.koitharu.kotatsu.reader.domain

data class MangaTranslationPreference(
	val branch: String,
	val priority: Int,
	val isEnabled: Boolean,
	val lastUsed: Long?,
	val chapterCount: Int = 0, // For display purposes
) {
	companion object {
		const val DEFAULT_PRIORITY = Int.MAX_VALUE
	}
}

data class MangaTranslationSettings(
	val preferences: List<MangaTranslationPreference>,
	val skipDecimalChapters: Boolean = false,
)