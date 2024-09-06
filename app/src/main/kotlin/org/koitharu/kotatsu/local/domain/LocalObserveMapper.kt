package org.koitharu.kotatsu.local.domain

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.transformLatest
import org.koitharu.kotatsu.core.model.isLocal
import org.koitharu.kotatsu.local.data.LocalMangaRepository
import org.koitharu.kotatsu.parsers.model.Manga
import java.util.Collections
import java.util.WeakHashMap

abstract class LocalObserveMapper<E : Any, R : Any>(
	private val localMangaRepository: LocalMangaRepository,
	private val limitStep: Int,
) {

	private val cache = Collections.synchronizedMap(WeakHashMap<Manga, R?>())

	protected fun observe(limit: Int, observer: (limit: Int) -> Flow<List<E>>): Flow<List<R>> {
		val floatingLimit = MutableStateFlow(limit)
		return floatingLimit.flatMapLatest { l ->
			observer(l)
				.transformLatest { fullList ->
					val mapped = fullList.mapToLocal(cache)
					if (mapped.size < limit && fullList.size == l) {
						floatingLimit.value += limitStep
					} else {
						emit(mapped.take(limit))
					}
				}.distinctUntilChanged()
		}
	}

	private suspend fun List<E>.mapToLocal(cache: MutableMap<Manga, R?>): List<R> = coroutineScope {
		val dispatcher = Dispatchers.IO.limitedParallelism(8)
		map { item ->
			val m = toManga(item)
			if (cache.contains(m)) {
				CompletableDeferred(cache[m])
			} else async(dispatcher) {
				val mapped = if (m.isLocal) {
					m
				} else {
					localMangaRepository.findSavedManga(m)?.manga
				}
				mapped?.let { mm -> toResult(item, mm) }.also { cache[m] = it }
			}
		}.awaitAll().filterNotNull()
	}

	protected abstract fun toManga(e: E): Manga

	protected abstract fun toResult(e: E, manga: Manga): R
}
