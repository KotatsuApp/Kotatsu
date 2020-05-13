package org.koitharu.kotatsu.core.parser.site

import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.domain.MangaLoaderContext

class SelfMangaRepository(loaderContext: MangaLoaderContext) : GroupleRepository(loaderContext) {

	override val defaultDomain = "selfmanga.ru"
	override val source = MangaSource.SELFMANGA
}