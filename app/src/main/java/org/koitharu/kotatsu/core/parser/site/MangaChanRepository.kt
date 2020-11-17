package org.koitharu.kotatsu.core.parser.site

import org.koitharu.kotatsu.base.domain.MangaLoaderContext
import org.koitharu.kotatsu.core.model.MangaSource

class MangaChanRepository(loaderContext: MangaLoaderContext) : ChanRepository(loaderContext) {

	override val defaultDomain = "manga-chan.me"
	override val source = MangaSource.MANGACHAN
}