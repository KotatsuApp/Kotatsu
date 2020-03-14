package org.koitharu.kotatsu.core.parser.site

import org.koitharu.kotatsu.core.model.MangaSource

class YaoiChanRepository : ChanRepository() {

	override val source = MangaSource.YAOICHAN
	override val defaultDomain = "yaoi-chan.me"
}