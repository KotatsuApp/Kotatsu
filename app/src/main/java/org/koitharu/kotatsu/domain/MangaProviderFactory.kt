package org.koitharu.kotatsu.domain

import org.koin.core.KoinComponent
import org.koin.core.get
import org.koin.core.inject
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.core.parser.LocalMangaRepository
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.prefs.AppSettings
import java.lang.ref.WeakReference
import java.util.*

object MangaProviderFactory : KoinComponent {

	private val loaderContext by inject<MangaLoaderContext>()
	private val cache = EnumMap<MangaSource, WeakReference<MangaRepository>>(MangaSource::class.java)

	fun getSources(includeHidden: Boolean): List<MangaSource> {
		val settings = get<AppSettings>()
		val list = MangaSource.values().toList() - MangaSource.LOCAL
		val order = settings.sourcesOrder
		val hidden = settings.hiddenSources
		val sorted = list.sortedBy { x ->
			val e = order.indexOf(x.ordinal)
			if (e == -1) order.size + x.ordinal else e
		}
		return if (includeHidden) {
			sorted
		} else {
			sorted.filterNot { x ->
				x.name in hidden
			}
		}
	}

	fun createLocal(): LocalMangaRepository =
		(cache[MangaSource.LOCAL]?.get() as? LocalMangaRepository)
			?: LocalMangaRepository().also {
				cache[MangaSource.LOCAL] = WeakReference<MangaRepository>(it)
			}

	@Throws(Throwable::class)
	fun create(source: MangaSource): MangaRepository {
		cache[source]?.get()?.let {
			return it
		}
		val instance = try {
			source.cls.getDeclaredConstructor(MangaLoaderContext::class.java)
				.newInstance(loaderContext)
		} catch (e: NoSuchMethodException) {
			source.cls.newInstance()
		}
		cache[source] = WeakReference<MangaRepository>(instance)
		return instance
	}
}