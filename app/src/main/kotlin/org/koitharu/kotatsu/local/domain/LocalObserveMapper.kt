package org.koitharu.kotatsu.local.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import org.koitharu.kotatsu.core.model.isLocal
import org.koitharu.kotatsu.local.data.index.LocalMangaIndex
import org.koitharu.kotatsu.parsers.model.Manga

abstract class LocalObserveMapper<E : Any, R : Any>(
	private val localMangaIndex: LocalMangaIndex,
) {

	protected fun Flow<Collection<E>>.mapToLocal() = onStart {
		localMangaIndex.updateIfRequired()
	}.mapLatest {
		it.mapToLocal()
	}

	private suspend fun Collection<E>.mapToLocal(): List<R> = coroutineScope {
		val dispatcher = Dispatchers.IO.limitedParallelism(6)
		map { item ->
			val m = toManga(item)
			async(dispatcher) {
				val mapped = if (m.isLocal) {
					m
				} else {
					localMangaIndex.get(m.id, withDetails = false)?.manga
				}
				mapped?.let { mm -> toResult(item, mm) }
			}
		}.awaitAll().filterNotNull()
	}

	protected abstract fun toManga(e: E): Manga

	protected abstract fun toResult(e: E, manga: Manga): R
}
