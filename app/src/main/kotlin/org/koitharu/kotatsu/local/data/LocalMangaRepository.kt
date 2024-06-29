package org.koitharu.kotatsu.local.data

import android.net.Uri
import androidx.core.net.toFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import org.koitharu.kotatsu.core.model.LocalMangaSource
import org.koitharu.kotatsu.core.model.isLocal
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.util.AlphanumComparator
import org.koitharu.kotatsu.core.util.MultiMutex
import org.koitharu.kotatsu.core.util.ext.children
import org.koitharu.kotatsu.core.util.ext.deleteAwait
import org.koitharu.kotatsu.core.util.ext.filterWith
import org.koitharu.kotatsu.core.util.ext.printStackTraceDebug
import org.koitharu.kotatsu.local.data.input.LocalMangaInput
import org.koitharu.kotatsu.local.data.output.LocalMangaOutput
import org.koitharu.kotatsu.local.data.output.LocalMangaUtil
import org.koitharu.kotatsu.local.domain.model.LocalManga
import org.koitharu.kotatsu.parsers.model.ContentRating
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.parsers.model.MangaListFilter
import org.koitharu.kotatsu.parsers.model.MangaPage
import org.koitharu.kotatsu.parsers.model.MangaState
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.parsers.model.SortOrder
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
import java.io.File
import java.util.EnumSet
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

private const val MAX_PARALLELISM = 4

@Singleton
class LocalMangaRepository @Inject constructor(
	private val storageManager: LocalStorageManager,
	@LocalStorageChanges private val localStorageChanges: MutableSharedFlow<LocalManga?>,
	private val settings: AppSettings,
) : MangaRepository {

	override val source = LocalMangaSource
	private val locks = MultiMutex<Long>()
	private val localMappingCache = LocalMangaMappingCache()

	override val isMultipleTagsSupported: Boolean = true
	override val isTagsExclusionSupported: Boolean = true
	override val isSearchSupported: Boolean = true
	override val sortOrders: Set<SortOrder> = EnumSet.of(SortOrder.ALPHABETICAL, SortOrder.RATING, SortOrder.NEWEST)
	override val states = emptySet<MangaState>()
	override val contentRatings = emptySet<ContentRating>()

	override var defaultSortOrder: SortOrder
		get() = settings.localListOrder
		set(value) {
			settings.localListOrder = value
		}

	override suspend fun getList(offset: Int, filter: MangaListFilter?): List<Manga> {
		if (offset > 0) {
			return emptyList()
		}
		val list = getRawList()
		when (filter) {
			is MangaListFilter.Search -> {
				list.retainAll { x -> x.isMatchesQuery(filter.query) }
			}

			is MangaListFilter.Advanced -> {
				if (filter.tags.isNotEmpty()) {
					list.retainAll { x -> x.containsTags(filter.tags) }
				}
				if (filter.tagsExclude.isNotEmpty()) {
					list.removeAll { x -> x.containsAnyTag(filter.tags) }
				}
				when (filter.sortOrder) {
					SortOrder.ALPHABETICAL -> list.sortWith(compareBy(AlphanumComparator()) { x -> x.manga.title })
					SortOrder.RATING -> list.sortByDescending { it.manga.rating }
					SortOrder.NEWEST,
					SortOrder.UPDATED,
					-> list.sortByDescending { it.createdAt }

					else -> Unit
				}
			}

			null -> Unit
		}
		return list.unwrap()
	}

	override suspend fun getDetails(manga: Manga): Manga = when {
		!manga.isLocal -> requireNotNull(findSavedManga(manga)?.manga) {
			"Manga is not local or saved"
		}

		else -> LocalMangaInput.of(manga).getManga().manga
	}

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		return LocalMangaInput.of(chapter).getPages(chapter)
	}

	suspend fun delete(manga: Manga): Boolean {
		val file = Uri.parse(manga.url).toFile()
		val result = file.deleteAwait()
		if (result) {
			localStorageChanges.emit(null)
		}
		return result
	}

	suspend fun deleteChapters(manga: Manga, ids: Set<Long>) {
		lockManga(manga.id)
		try {
			val subject = if (manga.isLocal) manga else checkNotNull(findSavedManga(manga)) {
				"Manga is not stored on local storage"
			}.manga
			LocalMangaUtil(subject).deleteChapters(ids)
			localStorageChanges.emit(LocalManga(subject))
		} finally {
			unlockManga(manga.id)
		}
	}

	suspend fun getRemoteManga(localManga: Manga): Manga? {
		return runCatchingCancellable {
			LocalMangaInput.of(localManga).getMangaInfo()?.takeUnless { it.isLocal }
		}.onFailure {
			it.printStackTraceDebug()
		}.getOrNull()
	}

	suspend fun findSavedManga(remoteManga: Manga): LocalManga? = runCatchingCancellable {
		// very fast path
		localMappingCache.get(remoteManga.id)?.let {
			return@runCatchingCancellable it
		}
		// fast path
		LocalMangaInput.find(storageManager.getReadableDirs(), remoteManga)?.let {
			return it.getManga()
		}
		// slow path
		val files = getAllFiles()
		return channelFlow {
			for (file in files) {
				launch {
					val mangaInput = LocalMangaInput.ofOrNull(file)
					runCatchingCancellable {
						val mangaInfo = mangaInput?.getMangaInfo()
						if (mangaInfo != null && mangaInfo.id == remoteManga.id) {
							send(mangaInput)
						}
					}.onFailure {
						it.printStackTraceDebug()
					}
				}
			}
		}.firstOrNull()?.getManga()
	}.onSuccess { x: LocalManga? ->
		localMappingCache[remoteManga.id] = x
	}.onFailure {
		it.printStackTraceDebug()
	}.getOrNull()

	override suspend fun getPageUrl(page: MangaPage) = page.url

	override suspend fun getTags() = emptySet<MangaTag>()

	override suspend fun getLocales() = emptySet<Locale>()

	override suspend fun getRelated(seed: Manga): List<Manga> = emptyList()

	suspend fun getOutputDir(manga: Manga): File? {
		val defaultDir = storageManager.getDefaultWriteableDir()
		if (defaultDir != null && LocalMangaOutput.get(defaultDir, manga) != null) {
			return defaultDir
		}
		return storageManager.getWriteableDirs()
			.firstOrNull {
				LocalMangaOutput.get(it, manga) != null
			} ?: defaultDir
	}

	suspend fun cleanup(): Boolean {
		if (locks.isNotEmpty()) {
			return false
		}
		val dirs = storageManager.getWriteableDirs()
		runInterruptible(Dispatchers.IO) {
			dirs.flatMap { dir ->
				dir.children().filterWith(TempFileFilter())
			}.forEach { file ->
				file.deleteRecursively()
			}
		}
		return true
	}

	suspend fun lockManga(id: Long) {
		locks.lock(id)
	}

	fun unlockManga(id: Long) {
		locks.unlock(id)
	}

	private suspend fun getRawList(): ArrayList<LocalManga> {
		val files = getAllFiles().toList() // TODO remove toList()
		return coroutineScope {
			val dispatcher = Dispatchers.IO.limitedParallelism(MAX_PARALLELISM)
			files.map { file ->
				async(dispatcher) {
					runCatchingCancellable { LocalMangaInput.ofOrNull(file)?.getManga() }.getOrNull()
				}
			}.awaitAll()
		}.filterNotNullTo(ArrayList(files.size))
	}

	private suspend fun getAllFiles() = storageManager.getReadableDirs()
		.asSequence()
		.flatMap { dir ->
			dir.children().filterNot { it.isHidden }
		}

	private fun Collection<LocalManga>.unwrap(): List<Manga> = map { it.manga }
}
