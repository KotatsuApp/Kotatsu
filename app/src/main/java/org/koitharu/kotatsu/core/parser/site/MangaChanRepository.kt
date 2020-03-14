package org.koitharu.kotatsu.core.parser.site

import org.koitharu.kotatsu.core.model.MangaSource

class MangaChanRepository : ChanRepository() {

	override val defaultDomain = "manga-chan.me"
	override val source = MangaSource.MANGACHAN
}