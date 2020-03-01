package org.koitharu.kotatsu.core.parser.site

import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.domain.MangaLoaderContext

class MangaChanRepository(loaderContext: MangaLoaderContext) :
	ChanRepository(MangaSource.MANGACHAN, loaderContext) {

	override val domain: String = "manga-chan.me"
}