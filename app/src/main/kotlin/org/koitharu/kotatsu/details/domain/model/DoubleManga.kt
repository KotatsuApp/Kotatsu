package org.koitharu.kotatsu.details.domain.model

import org.koitharu.kotatsu.core.model.findById
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.reader.data.filterChapters

data class DoubleManga(
	private val remoteManga: Result<Manga>?,
	private val localManga: Result<Manga>?,
) {

	constructor(manga: Manga) : this(
		remoteManga = if (manga.source != MangaSource.LOCAL) Result.success(manga) else null,
		localManga = if (manga.source == MangaSource.LOCAL) Result.success(manga) else null,
	)

	val remote: Manga?
		get() = remoteManga?.getOrNull()

	val local: Manga?
		get() = localManga?.getOrNull()

	val any: Manga?
		get() = remote ?: local

	val hasRemote: Boolean
		get() = remoteManga?.isSuccess == true

	val hasLocal: Boolean
		get() = localManga?.isSuccess == true

	val chapters: List<MangaChapter>? by lazy(LazyThreadSafetyMode.PUBLICATION) {
		mergeChapters()
	}

	fun hasChapter(id: Long): Boolean {
		return local?.chapters?.findById(id) != null || remote?.chapters?.findById(id) != null
	}

	fun requireAny(): Manga {
		val result = remoteManga?.getOrNull() ?: localManga?.getOrNull()
		if (result != null) {
			return result
		}
		throw (
			remoteManga?.exceptionOrNull()
				?: localManga?.exceptionOrNull()
				?: IllegalStateException("No online either local manga available")
			)
	}

	fun filterChapters(branch: String?) = DoubleManga(
		remoteManga?.map { it.filterChapters(branch) },
		localManga?.map { it.filterChapters(branch) },
	)

	private fun mergeChapters(): List<MangaChapter>? {
		val remoteChapters = remote?.chapters
		val localChapters = local?.chapters
		if (localChapters == null && remoteChapters == null) {
			return null
		}
		val localMap = if (!localChapters.isNullOrEmpty()) {
			localChapters.associateByTo(LinkedHashMap(localChapters.size)) { it.id }
		} else {
			null
		}
		val result = ArrayList<MangaChapter>(maxOf(remoteChapters?.size ?: 0, localChapters?.size ?: 0))
		remoteChapters?.forEach { r ->
			localMap?.remove(r.id)?.let { l ->
				result.add(l)
			} ?: result.add(r)
		}
		localMap?.values?.let {
			result.addAll(it)
		}
		return result
	}
}
