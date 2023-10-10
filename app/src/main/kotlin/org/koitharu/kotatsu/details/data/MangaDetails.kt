package org.koitharu.kotatsu.details.data

import org.koitharu.kotatsu.core.model.isLocal
import org.koitharu.kotatsu.local.domain.model.LocalManga
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.reader.data.filterChapters

data class MangaDetails(
	private val manga: Manga,
	private val localManga: LocalManga?,
	val description: CharSequence?,
	val isLoaded: Boolean,
) {

	val id: Long
		get() = manga.id

	val chapters: Map<String?, List<MangaChapter>> = manga.chapters?.groupBy { it.branch }.orEmpty()

	val branches: Set<String?>
		get() = chapters.keys

	val allChapters: List<MangaChapter>
		get() = manga.chapters.orEmpty()

	val isLocal
		get() = manga.isLocal

	val local: LocalManga?
		get() = localManga ?: if (manga.isLocal) LocalManga(manga) else null

	fun toManga() = manga

	fun filterChapters(branch: String?) = MangaDetails(
		manga = manga.filterChapters(branch),
		localManga = localManga?.run {
			copy(manga = manga.filterChapters(branch))
		},
		description = description,
		isLoaded = isLoaded,
	)
}
