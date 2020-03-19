package org.koitharu.kotatsu.domain

import org.koin.core.KoinComponent
import org.koin.core.get
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.core.parser.LocalMangaRepository
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.prefs.AppSettings

object MangaProviderFactory : KoinComponent {

	fun getSources(includeHidden: Boolean): List<MangaSource> {
		val settings = get<AppSettings>()
		val list = MangaSource.values().toList() - MangaSource.LOCAL
		val order = settings.sourcesOrder
		val hidden = settings.hiddenSources
		val sorted = list.sortedBy { x ->
			val e = order.indexOf(x.ordinal)
			if (e == -1) order.size + x.ordinal else e
		}
		return if(includeHidden) {
			sorted
		} else {
			sorted.filterNot { x ->
				x.name in hidden
			}
		}
	}

	fun createLocal() = LocalMangaRepository()

	fun create(source: MangaSource): MangaRepository {
		return source.cls.newInstance()
	}
}