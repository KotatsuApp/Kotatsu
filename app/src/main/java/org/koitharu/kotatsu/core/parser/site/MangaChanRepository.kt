package org.koitharu.kotatsu.core.parser.site

import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.domain.MangaLoaderContext

class MangaChanRepository(loaderContext: MangaLoaderContext) : ChanRepository(loaderContext) {

	override val defaultDomain = "manga-chan.me"
	override val source = MangaSource.MANGACHAN
}