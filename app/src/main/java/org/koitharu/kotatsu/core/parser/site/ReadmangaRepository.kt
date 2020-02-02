package org.koitharu.kotatsu.core.parser.site

import androidx.core.text.parseAsHtml
import org.koitharu.kotatsu.core.model.*
import org.koitharu.kotatsu.core.parser.BaseMangaRepository
import org.koitharu.kotatsu.domain.MangaLoaderContext
import org.koitharu.kotatsu.core.exceptions.ParseException
import org.koitharu.kotatsu.utils.ext.*

class ReadmangaRepository(loaderContext: MangaLoaderContext) :
	GroupleRepository(MangaSource.READMANGA_RU, loaderContext) {

	override val domain = "readmanga.me"

}