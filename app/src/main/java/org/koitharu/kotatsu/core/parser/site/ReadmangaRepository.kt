package org.koitharu.kotatsu.core.parser.site

import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.domain.MangaLoaderContext

class ReadmangaRepository(loaderContext: MangaLoaderContext) :
	GroupleRepository(MangaSource.READMANGA_RU, loaderContext) {

	override val domain = "readmanga.me"

}