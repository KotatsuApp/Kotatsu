package org.koitharu.kotatsu.core.parser

import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaParser
import org.koitharu.kotatsu.parsers.model.MangaSource

fun MangaParser(source: MangaSource, loaderContext: MangaLoaderContext): MangaParser {
	return if (source == MangaSource.DUMMY) {
		DummyParser(loaderContext)
	} else {
		loaderContext.newParserInstance(source)
	}
}
