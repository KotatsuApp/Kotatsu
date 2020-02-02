package org.koitharu.kotatsu.domain

import org.koin.core.KoinComponent
import org.koin.core.get
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.core.parser.MangaRepository

object MangaProviderFactory : KoinComponent {

	private val loaderContext get() = get<MangaLoaderContext>()

	fun create(source: MangaSource): MangaRepository {
		val constructor = source.cls.getConstructor(MangaLoaderContext::class.java)
		return constructor.newInstance(loaderContext)
	}
}