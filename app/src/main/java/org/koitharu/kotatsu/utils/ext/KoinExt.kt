package org.koitharu.kotatsu.utils.ext

import android.content.ComponentCallbacks
import org.koin.android.ext.android.get
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.qualifier.named
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.core.parser.MangaRepository

@Suppress("NOTHING_TO_INLINE")
inline fun ComponentCallbacks.mangaRepositoryOf(source: MangaSource): MangaRepository {
	return get(named(source))
}

@Suppress("NOTHING_TO_INLINE")
inline fun KoinComponent.mangaRepositoryOf(source: MangaSource): MangaRepository {
	return get(named(source))
}