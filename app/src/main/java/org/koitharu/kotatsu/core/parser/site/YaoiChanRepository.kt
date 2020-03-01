package org.koitharu.kotatsu.core.parser.site

import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.domain.MangaLoaderContext

class YaoiChanRepository(loaderContext: MangaLoaderContext) :
	ChanRepository(MangaSource.YAOICHAN, loaderContext) {

	override val domain: String = "yaoi-chan.me"
}