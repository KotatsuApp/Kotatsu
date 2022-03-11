package org.koitharu.kotatsu.core.parser.site

import org.jsoup.nodes.Document
import org.koitharu.kotatsu.base.domain.MangaLoaderContext
import org.koitharu.kotatsu.core.model.MangaSource

class HentaiLibRepository(loaderContext: MangaLoaderContext) : MangaLibRepository(loaderContext) {

	override val defaultDomain = "hentailib.me"

	override val source = MangaSource.HENTAILIB

	override fun isNsfw(doc: Document) = true
}
