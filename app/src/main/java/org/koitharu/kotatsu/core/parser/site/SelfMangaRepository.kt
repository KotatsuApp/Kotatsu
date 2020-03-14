package org.koitharu.kotatsu.core.parser.site

import org.koitharu.kotatsu.core.model.MangaSource

class SelfMangaRepository : GroupleRepository() {

	override val defaultDomain = "selfmanga.ru"
	override val source = MangaSource.SELFMANGA
}