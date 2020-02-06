package org.koitharu.kotatsu.domain

import org.koin.core.KoinComponent
import org.koin.core.inject
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.core.parser.MangaRepository

object MangaProviderFactory : KoinComponent {

	private val loaderContext by inject<MangaLoaderContext>()

	fun create(source: MangaSource): MangaRepository {
		val constructor = source.cls.getConstructor(MangaLoaderContext::class.java)
		return constructor.newInstance(loaderContext)
	}
}