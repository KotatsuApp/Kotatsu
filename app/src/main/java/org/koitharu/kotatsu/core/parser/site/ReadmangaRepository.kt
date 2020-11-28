package org.koitharu.kotatsu.core.parser.site

import org.koitharu.kotatsu.base.domain.MangaLoaderContext
import org.koitharu.kotatsu.core.model.MangaSource

class ReadmangaRepository(loaderContext: MangaLoaderContext) : GroupleRepository(loaderContext) {

	override val defaultDomain = "readmanga.live"
	override val source = MangaSource.READMANGA_RU
}