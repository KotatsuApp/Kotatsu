package org.koitharu.kotatsu.base.domain

import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.core.prefs.AppSettings

object MangaProviderFactory {

	fun getSources(settings: AppSettings, includeHidden: Boolean): List<MangaSource> {
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