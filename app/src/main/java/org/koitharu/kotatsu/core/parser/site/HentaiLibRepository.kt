package org.koitharu.kotatsu.core.parser.site

import org.koitharu.kotatsu.core.model.MangaSource

class HentaiLibRepository : MangaLibRepository() {

	protected override val defaultDomain = "hentailib.me"

	override val source = MangaSource.HENTAILIB

}