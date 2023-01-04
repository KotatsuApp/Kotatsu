package org.koitharu.kotatsu.core.cache

import kotlinx.coroutines.Deferred
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaPage
import org.koitharu.kotatsu.parsers.model.MangaSource

interface ContentCache {

	suspend fun getDetails(source: MangaSource, url: String): Manga?

	fun putDetails(source: MangaSource, url: String, details: Deferred<Manga>)

	suspend fun getPages(source: MangaSource, url: String): List<MangaPage>?

	fun putPages(source: MangaSource, url: String, pages: Deferred<List<MangaPage>>)

	data class Key(
		val source: MangaSource,
		val url: String,
	)
}
