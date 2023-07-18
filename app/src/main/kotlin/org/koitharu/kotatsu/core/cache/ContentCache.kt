package org.koitharu.kotatsu.core.cache

import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaPage
import org.koitharu.kotatsu.parsers.model.MangaSource

interface ContentCache {

	val isCachingEnabled: Boolean

	suspend fun getDetails(source: MangaSource, url: String): Manga?

	fun putDetails(source: MangaSource, url: String, details: SafeDeferred<Manga>)

	suspend fun getPages(source: MangaSource, url: String): List<MangaPage>?

	fun putPages(source: MangaSource, url: String, pages: SafeDeferred<List<MangaPage>>)

	suspend fun getRelatedManga(source: MangaSource, url: String): List<Manga>?

	fun putRelatedManga(source: MangaSource, url: String, related: SafeDeferred<List<Manga>>)

	class Key(
		val source: MangaSource,
		val url: String,
	) {

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false

			other as Key

			if (source != other.source) return false
			return url == other.url
		}

		override fun hashCode(): Int {
			var result = source.hashCode()
			result = 31 * result + url.hashCode()
			return result
		}
	}
}
