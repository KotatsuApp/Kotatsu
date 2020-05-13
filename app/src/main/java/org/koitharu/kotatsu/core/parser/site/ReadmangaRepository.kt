package org.koitharu.kotatsu.core.parser.site

import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.domain.MangaLoaderContext

class ReadmangaRepository(loaderContext: MangaLoaderContext) : GroupleRepository(loaderContext) {

	override val defaultDomain = "readmanga.me"
	override val source = MangaSource.READMANGA_RU
}