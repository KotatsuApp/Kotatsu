package org.koitharu.kotatsu.core.cache

import kotlinx.coroutines.Deferred
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaPage
import org.koitharu.kotatsu.parsers.model.MangaSource

class StubContentCache : ContentCache {

	override suspend fun getDetails(source: MangaSource, url: String): Manga? = null

	override fun putDetails(source: MangaSource, url: String, details: Deferred<Manga>) = Unit

	override suspend fun getPages(source: MangaSource, url: String): List<MangaPage>? = null

	override fun putPages(source: MangaSource, url: String, pages: Deferred<List<MangaPage>>) = Unit
}
