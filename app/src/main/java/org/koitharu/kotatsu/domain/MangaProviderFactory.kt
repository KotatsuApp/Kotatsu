package org.koitharu.kotatsu.domain

import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koitharu.kotatsu.core.model.MangaSource
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
		return if (includeHidden) {
			sorted
		} else {
			sorted.filterNot { x ->
				x.name in hidden
			}
		}
	}
}