package org.koitharu.kotatsu.core.parser.site

import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.domain.MangaLoaderContext

class MintMangaRepository(loaderContext: MangaLoaderContext) : GroupleRepository(loaderContext) {

	override val source = MangaSource.MINTMANGA
	override val defaultDomain: String = "mintmanga.live"
}