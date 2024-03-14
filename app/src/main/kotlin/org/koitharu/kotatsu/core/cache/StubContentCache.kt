package org.koitharu.kotatsu.core.cache

import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaPage
import org.koitharu.kotatsu.parsers.model.MangaSource

class StubContentCache : ContentCache {

	override val isCachingEnabled: Boolean = false

	override suspend fun getDetails(source: MangaSource, url: String): Manga? = null

	override fun putDetails(source: MangaSource, url: String, details: SafeDeferred<Manga>) = Unit

	override suspend fun getPages(source: MangaSource, url: String): List<MangaPage>? = null

	override fun putPages(source: MangaSource, url: String, pages: SafeDeferred<List<MangaPage>>) = Unit

	override suspend fun getRelatedManga(source: MangaSource, url: String): List<Manga>? = null

	override fun putRelatedManga(source: MangaSource, url: String, related: SafeDeferred<List<Manga>>) = Unit

	override fun clear(source: MangaSource) = Unit
}
