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

	data class Key(
		val source: MangaSource,
		val url: String,
	)
}
