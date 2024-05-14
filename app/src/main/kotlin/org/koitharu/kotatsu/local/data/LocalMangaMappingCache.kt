package org.koitharu.kotatsu.local.data

import androidx.collection.MutableLongObjectMap
import org.koitharu.kotatsu.core.util.ext.printStackTraceDebug
import org.koitharu.kotatsu.local.data.input.LocalMangaInput
import org.koitharu.kotatsu.local.domain.model.LocalManga
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
import java.io.File

class LocalMangaMappingCache {

	private val map = MutableLongObjectMap<File>()

	suspend fun get(mangaId: Long): LocalManga? {
		val file = synchronized(this) {
			map[mangaId]
		} ?: return null
		return runCatchingCancellable {
			LocalMangaInput.of(file).getManga()
		}.onFailure {
			it.printStackTraceDebug()
		}.getOrNull()
	}

	operator fun set(mangaId: Long, localManga: LocalManga?) = synchronized(this) {
		if (localManga == null) {
			map.remove(mangaId)
		} else {
			map[mangaId] = localManga.file
		}
	}
}
