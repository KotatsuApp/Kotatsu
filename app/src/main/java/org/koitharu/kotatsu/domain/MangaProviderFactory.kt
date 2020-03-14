package org.koitharu.kotatsu.domain

import org.koin.core.KoinComponent
import org.koin.core.get
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.core.parser.LocalMangaRepository
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.prefs.AppSettings

object MangaProviderFactory : KoinComponent {

	val sources: List<MangaSource>
		get() {
			val list = MangaSource.values().toList() - MangaSource.LOCAL
			val order = get<AppSettings>().sourcesOrder
			return list.sortedBy { x ->
				val e = order.indexOf(x.ordinal)
				if (e == -1) order.size + x.ordinal else e
			}
		}

	fun createLocal() = LocalMangaRepository()

	fun create(source: MangaSource): MangaRepository {
		return source.cls.newInstance()
	}
}