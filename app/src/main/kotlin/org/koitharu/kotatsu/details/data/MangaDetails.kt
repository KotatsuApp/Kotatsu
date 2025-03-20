package org.koitharu.kotatsu.details.data

import org.koitharu.kotatsu.core.model.getLocale
import org.koitharu.kotatsu.core.model.isLocal
import org.koitharu.kotatsu.local.domain.model.LocalManga
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.parsers.util.ifNullOrEmpty
import org.koitharu.kotatsu.parsers.util.nullIfEmpty
import org.koitharu.kotatsu.reader.data.filterChapters
import java.util.Locale

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

	val allChapters: List<MangaChapter> by lazy { mergeChapters() }

	val isLocal
		get() = manga.isLocal

	val local: LocalManga?
		get() = localManga ?: if (manga.isLocal) LocalManga(manga) else null

	val coverUrl: String?
		get() = manga.largeCoverUrl
			.ifNullOrEmpty { manga.coverUrl }
			.ifNullOrEmpty { localManga?.manga?.coverUrl }
			?.nullIfEmpty()

	fun toManga() = manga

	fun getLocale(): Locale? {
		findAppropriateLocale(chapters.keys.singleOrNull())?.let {
			return it
		}
		return manga.source.getLocale()
	}

	fun filterChapters(branch: String?) = MangaDetails(
		manga = manga.filterChapters(branch),
		localManga = localManga?.run {
			copy(manga = manga.filterChapters(branch))
		},
		description = description,
		isLoaded = isLoaded,
	)

	private fun mergeChapters(): List<MangaChapter> {
		val chapters = manga.chapters
		val localChapters = local?.manga?.chapters.orEmpty()
		if (chapters.isNullOrEmpty()) {
			return localChapters
		}
		val localMap = if (localChapters.isNotEmpty()) {
			localChapters.associateByTo(LinkedHashMap(localChapters.size)) { it.id }
		} else {
			null
		}
		val result = ArrayList<MangaChapter>(chapters.size)
		for (chapter in chapters) {
			val local = localMap?.remove(chapter.id)
			result += local ?: chapter
		}
		if (!localMap.isNullOrEmpty()) {
			result.addAll(localMap.values)
		}
		return result
	}

	private fun findAppropriateLocale(name: String?): Locale? {
		if (name.isNullOrEmpty()) {
			return null
		}
		return Locale.getAvailableLocales().find { lc ->
			name.contains(lc.getDisplayName(lc), ignoreCase = true) ||
				name.contains(lc.getDisplayName(Locale.ENGLISH), ignoreCase = true) ||
				name.contains(lc.getDisplayLanguage(lc), ignoreCase = true) ||
				name.contains(lc.getDisplayLanguage(Locale.ENGLISH), ignoreCase = true)
		}
	}
}
