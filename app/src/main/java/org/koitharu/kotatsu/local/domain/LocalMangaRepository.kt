package org.koitharu.kotatsu.local.domain

import android.net.Uri
import androidx.core.net.toFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.runInterruptible
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.local.data.LocalManga
import org.koitharu.kotatsu.local.data.LocalStorageManager
import org.koitharu.kotatsu.local.data.TempFileFilter
import org.koitharu.kotatsu.local.data.input.LocalMangaInput
import org.koitharu.kotatsu.local.data.output.LocalMangaUtil
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.parsers.model.MangaPage
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.parsers.model.SortOrder
import org.koitharu.kotatsu.utils.AlphanumComparator
import org.koitharu.kotatsu.utils.CompositeMutex
import org.koitharu.kotatsu.utils.ext.deleteAwait
import org.koitharu.kotatsu.utils.ext.printStackTraceDebug
import org.koitharu.kotatsu.utils.ext.runCatchingCancellable
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private const val MAX_PARALLELISM = 4

@Singleton
class LocalMangaRepository @Inject constructor(private val storageManager: LocalStorageManager) : MangaRepository {

	override val source = MangaSource.LOCAL
	private val locks = CompositeMutex<Long>()

	override suspend fun getList(offset: Int, query: String): List<Manga> {
		if (offset > 0) {
			return emptyList()
		}
		val list = getRawList()
		if (query.isNotEmpty()) {
			list.retainAll { x -> x.isMatchesQuery(query) }
		}
		return list.unwrap()
	}

	override suspend fun getList(offset: Int, tags: Set<MangaTag>?, sortOrder: SortOrder?): List<Manga> {
		if (offset > 0) {
			return emptyList()
		}
		val list = getRawList()
		if (!tags.isNullOrEmpty()) {
			list.retainAll { x -> x.containsTags(tags) }
		}
		when (sortOrder) {
			SortOrder.ALPHABETICAL -> list.sortWith(compareBy(AlphanumComparator()) { x -> x.manga.title })
			SortOrder.RATING -> list.sortByDescending { it.manga.rating }
			SortOrder.NEWEST,
			SortOrder.UPDATED,
			-> list.sortByDescending { it.createdAt }

			else -> Unit
		}
		return list.unwrap()
	}

	override suspend fun getDetails(manga: Manga): Manga = when {
		manga.source != MangaSource.LOCAL -> requireNotNull(findSavedManga(manga)?.manga) {
			"Manga is not local or saved"
		}

		else -> LocalMangaInput.of(manga).getManga().manga
	}

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		return LocalMangaInput.of(chapter).getPages(chapter)
	}

	suspend fun delete(manga: Manga): Boolean {
		val file = Uri.parse(manga.url).toFile()
		return file.deleteAwait()
	}

	suspend fun deleteChapters(manga: Manga, ids: Set<Long>) {
		lockManga(manga.id)
		try {
			LocalMangaUtil(manga).deleteChapters(ids)
		} finally {
			unlockManga(manga.id)
		}
	}

	suspend fun getRemoteManga(localManga: Manga): Manga? {
		return runCatchingCancellable {
			LocalMangaInput.of(localManga).getMangaInfo()
		}.onFailure {
			it.printStackTraceDebug()
		}.getOrNull()
	}

	suspend fun findSavedManga(remoteManga: Manga): LocalManga? {
		val files = getAllFiles()
		val input = files.firstNotNullOfOrNull { file ->
			LocalMangaInput.of(file).takeIf {
				runCatchingCancellable {
					it.getMangaInfo()
				}.getOrNull()?.id == remoteManga.id
			}
		}
		return input?.getManga()
	}

	suspend fun watchReadableDirs(): Flow<File> {
		val filter = TempFileFilter()
		val dirs = storageManager.getReadableDirs()
		return storageManager.observe(dirs)
			.filterNot { filter.accept(it, it.name) }
	}

	override val sortOrders = setOf(SortOrder.ALPHABETICAL, SortOrder.RATING)

	override suspend fun getPageUrl(page: MangaPage) = page.url

	override suspend fun getTags() = emptySet<MangaTag>()

	suspend fun getOutputDir(): File? {
		return storageManager.getDefaultWriteableDir()
	}

	suspend fun cleanup() {
		val dirs = storageManager.getWriteableDirs()
		runInterruptible(Dispatchers.IO) {
			dirs.flatMap { dir ->
				dir.listFiles(TempFileFilter())?.toList().orEmpty()
			}.forEach { file ->
				file.delete()
			}
		}
	}

	suspend fun lockManga(id: Long) {
		locks.lock(id)
	}

	fun unlockManga(id: Long) {
		locks.unlock(id)
	}

	private suspend fun getRawList(): ArrayList<LocalManga> {
		val files = getAllFiles()
		return coroutineScope {
			val dispatcher = Dispatchers.IO.limitedParallelism(MAX_PARALLELISM)
			files.map { file ->
				async(dispatcher) {
					runCatchingCancellable { LocalMangaInput.of(file).getManga() }.getOrNull()
				}
			}.awaitAll()
		}.filterNotNullTo(ArrayList(files.size))
	}

	private suspend fun getAllFiles() = storageManager.getReadableDirs().flatMap { dir ->
		dir.listFiles()?.toList().orEmpty()
	}

	private fun Collection<LocalManga>.unwrap(): List<Manga> = map { it.manga }
}
