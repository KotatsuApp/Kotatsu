package org.koitharu.kotatsu.core.parser.site

import org.koitharu.kotatsu.base.domain.MangaLoaderContext
import org.koitharu.kotatsu.core.model.MangaSource

class SelfMangaRepository(loaderContext: MangaLoaderContext) : GroupleRepository(loaderContext) {

	override val defaultDomain = "selfmanga.ru"
	override val source = MangaSource.SELFMANGA
}