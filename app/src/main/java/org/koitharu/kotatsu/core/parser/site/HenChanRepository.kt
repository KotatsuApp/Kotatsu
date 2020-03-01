package org.koitharu.kotatsu.core.parser.site

import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.domain.MangaLoaderContext

class HenChanRepository(loaderContext: MangaLoaderContext) :
	ChanRepository(MangaSource.HENCHAN, loaderContext) {

	override val domain: String = "h-chan.me"
}