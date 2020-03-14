package org.koitharu.kotatsu.core.parser.site

import org.koitharu.kotatsu.core.model.MangaSource

class MintMangaRepository : GroupleRepository() {

	override val source = MangaSource.MINTMANGA
	override val defaultDomain: String = "mintmanga.live"
}