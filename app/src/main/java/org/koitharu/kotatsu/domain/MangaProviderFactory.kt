package org.koitharu.kotatsu.domain

import org.koin.core.KoinComponent
import org.koin.core.get
import org.koin.core.inject
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.prefs.AppSettings

object MangaProviderFactory : KoinComponent {

	private val loaderContext by inject<MangaLoaderContext>()

	val sources: List<MangaSource>
		get() {
			val list = MangaSource.values().toList() - MangaSource.LOCAL
			val order = get<AppSettings>().sourcesOrder
			return list.sortedBy { x ->
				val e = order.indexOf(x.ordinal)
				if (e == -1) order.size + x.ordinal else e
			}
		}

	fun create(source: MangaSource): MangaRepository {
		val constructor = source.cls.getConstructor(MangaLoaderContext::class.java)
		return constructor.newInstance(loaderContext)
	}
}