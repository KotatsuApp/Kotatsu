package org.koitharu.kotatsu.details.ui.pager.pages

import coil.key.Keyer
import coil.request.Options
import org.koitharu.kotatsu.parsers.model.MangaPage

class MangaPageKeyer : Keyer<MangaPage> {

	override fun key(data: MangaPage, options: Options) = data.url
}
