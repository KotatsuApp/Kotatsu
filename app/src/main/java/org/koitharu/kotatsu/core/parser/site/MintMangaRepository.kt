package org.koitharu.kotatsu.core.parser.site

import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.domain.MangaLoaderContext

class MintMangaRepository(loaderContext: MangaLoaderContext) :
	GroupleRepository(MangaSource.MINTMANGA, loaderContext) {

	override val domain: String = "mintmanga.live"
}