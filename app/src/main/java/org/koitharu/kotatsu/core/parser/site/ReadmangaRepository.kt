package org.koitharu.kotatsu.core.parser.site

import org.koitharu.kotatsu.core.model.MangaSource

class ReadmangaRepository : GroupleRepository() {

	override val defaultDomain = "readmanga.me"
	override val source = MangaSource.READMANGA_RU
}