package org.koitharu.kotatsu.core.parser.site

import org.koitharu.kotatsu.core.model.MangaSource

class HenChanRepository : ChanRepository() {

	override val defaultDomain = "h-chan.me"
	override val source = MangaSource.HENCHAN
}