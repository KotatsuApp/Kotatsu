package org.koitharu.kotatsu.local.domain

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.annotation.WorkerThread
import androidx.collection.ArraySet
import androidx.core.net.toFile
import androidx.core.net.toUri
import kotlinx.coroutines.*
import org.koitharu.kotatsu.core.exceptions.UnsupportedFileException
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.local.data.CbzFilter
import org.koitharu.kotatsu.local.data.LocalStorageManager
import org.koitharu.kotatsu.local.data.MangaIndex
import org.koitharu.kotatsu.local.data.TempFileFilter
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.util.toCamelCase
import org.koitharu.kotatsu.utils.AlphanumComparator
import org.koitharu.kotatsu.utils.CompositeMutex
import org.koitharu.kotatsu.utils.ext.deleteAwait
import org.koitharu.kotatsu.utils.ext.longHashCode
import org.koitharu.kotatsu.utils.ext.readText
import org.koitharu.kotatsu.utils.ext.resolveName
import java.io.File
import java.io.IOException
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import kotlin.coroutines.CoroutineContext

private const val MAX_PARALLELISM = 4

class LocalMangaRepository(private val storageManager: LocalStorageManager) : MangaRepository {

	override val source = MangaSource.LOCAL
	private val filenameFilter = CbzFilter()
	private val locks = CompositeMutex<Long>()

	override suspend fun getList(
		offset: Int,
		query: String?,
		tags: Set<MangaTag>?,
		sortOrder: SortOrder?
	): List<Manga> {
		if (offset > 0) {
			return emptyList()
		}
		val files = getAllFiles()
		val list = coroutineScope {
			val dispatcher = Dispatchers.IO.limitedParallelism(MAX_PARALLELISM)
			files.map { file ->
				getFromFileAsync(file, dispatcher)
			}.awaitAll()
		}.filterNotNullTo(ArrayList(files.size))
		if (!query.isNullOrEmpty()) {
			list.retainAll { x ->
				x.title.contains(query, ignoreCase = true) ||
					x.altTitle?.contains(query, ignoreCase = true) == true
			}
		}
		if (!tags.isNullOrEmpty()) {
			list.retainAll { x ->
				x.tags.containsAll(tags)
			}
		}
		return list
	}

	override suspend fun getDetails(manga: Manga) = when {
		manga.source != MangaSource.LOCAL -> requireNotNull(findSavedManga(manga)) {
			"Manga is not local or saved"
		}
		else -> getFromFile(Uri.parse(manga.url).toFile())
	}

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		return runInterruptible(Dispatchers.IO) {
			val uri = Uri.parse(chapter.url)
			val file = uri.toFile()
			val zip = ZipFile(file)
			val index = zip.getEntry(CbzMangaOutput.ENTRY_NAME_INDEX)?.let(zip::readText)?.let(::MangaIndex)
			var entries = zip.entries().asSequence()
			entries = if (index != null) {
				val pattern = index.getChapterNamesPattern(chapter)
				entries.filter { x -> !x.isDirectory && x.name.substringBefore('.').matches(pattern) }
			} else {
				val parent = uri.fragment.orEmpty()
				entries.filter { x ->
					!x.isDirectory && x.name.substringBeforeLast(
						File.separatorChar,
						""
					) == parent
				}
			}
			entries
				.toList()
				.sortedWith(compareBy(AlphanumComparator()) { x -> x.name })
				.map { x ->
					val entryUri = zipUri(file, x.name)
					MangaPage(
						id = entryUri.longHashCode(),
						url = entryUri,
						preview = null,
						referer = chapter.url,
						source = MangaSource.LOCAL,
					)
				}
		}
	}

	suspend fun delete(manga: Manga): Boolean {
		val file = Uri.parse(manga.url).toFile()
		return file.deleteAwait()
	}

	suspend fun deleteChapters(manga: Manga, ids: Set<Long>) {
		lockManga(manga.id)
		try {
			runInterruptible(Dispatchers.IO) {
				val uri = Uri.parse(manga.url)
				val file = uri.toFile()
				val cbz = CbzMangaOutput(file, manga)
				CbzMangaOutput.filterChapters(cbz, ids)
			}
		} finally {
			unlockManga(manga.id)
		}
	}

	@WorkerThread
	@SuppressLint("DefaultLocale")
	fun getFromFile(file: File): Manga = ZipFile(file).use { zip ->
		val fileUri = file.toUri().toString()
		val entry = zip.getEntry(CbzMangaOutput.ENTRY_NAME_INDEX)
		val index = entry?.let(zip::readText)?.let(::MangaIndex)
		val info = index?.getMangaInfo()
		if (index != null && info != null) {
			return info.copy2(
				source = MangaSource.LOCAL,
				url = fileUri,
				coverUrl = zipUri(
					file,
					entryName = index.getCoverEntry() ?: findFirstImageEntry(zip.entries())?.name.orEmpty()
				),
				chapters = info.chapters?.map { c ->
					c.copy(url = fileUri, source = MangaSource.LOCAL)
				}
			)
		}
		// fallback
		val title = file.nameWithoutExtension.replace("_", " ").toCamelCase()
		val chapters = ArraySet<String>()
		for (x in zip.entries()) {
			if (!x.isDirectory) {
				chapters += x.name.substringBeforeLast(File.separatorChar, "")
			}
		}
		val uriBuilder = file.toUri().buildUpon()
		Manga(
			id = file.absolutePath.longHashCode(),
			title = title,
			url = fileUri,
			publicUrl = fileUri,
			source = MangaSource.LOCAL,
			coverUrl = zipUri(file, findFirstImageEntry(zip.entries())?.name.orEmpty()),
			chapters = chapters.sortedWith(AlphanumComparator()).mapIndexed { i, s ->
				MangaChapter(
					id = "$i$s".longHashCode(),
					name = s.ifEmpty { title },
					number = i + 1,
					source = MangaSource.LOCAL,
					uploadDate = 0L,
					url = uriBuilder.fragment(s).build().toString(),
					scanlator = null,
					branch = null,
				)
			},
			altTitle = null,
			rating = -1f,
			isNsfw = false,
			tags = setOf(),
			state = null,
			author = null,
			largeCoverUrl = null,
			description = null,
		)
	}

	suspend fun getRemoteManga(localManga: Manga): Manga? {
		val file = runCatching {
			Uri.parse(localManga.url).toFile()
		}.getOrNull() ?: return null
		return runInterruptible(Dispatchers.IO) {
			ZipFile(file).use { zip ->
				val entry = zip.getEntry(CbzMangaOutput.ENTRY_NAME_INDEX)
				val index = entry?.let(zip::readText)?.let(::MangaIndex)
				index?.getMangaInfo()
			}
		}
	}

	suspend fun findSavedManga(remoteManga: Manga): Manga? {
		val files = getAllFiles()
		return runInterruptible(Dispatchers.IO) {
			for (file in files) {
				val index = ZipFile(file).use { zip ->
					val entry = zip.getEntry(CbzMangaOutput.ENTRY_NAME_INDEX)
					entry?.let(zip::readText)?.let(::MangaIndex)
				} ?: continue
				val info = index.getMangaInfo() ?: continue
				if (info.id == remoteManga.id) {
					val fileUri = file.toUri().toString()
					return@runInterruptible info.copy2(
						source = MangaSource.LOCAL,
						url = fileUri,
						chapters = info.chapters?.map { c -> c.copy(url = fileUri) }
					)
				}
			}
			null
		}
	}

	private fun CoroutineScope.getFromFileAsync(
		file: File,
		context: CoroutineContext,
	): Deferred<Manga?> = async(context) {
		runInterruptible {
			runCatching { getFromFile(file) }.getOrNull()
		}
	}

	private fun zipUri(file: File, entryName: String) = "cbz://${file.path}#$entryName"

	private fun findFirstImageEntry(entries: Enumeration<out ZipEntry>): ZipEntry? {
		val list = entries.toList()
			.filterNot { it.isDirectory }
			.sortedWith(compareBy(AlphanumComparator()) { x -> x.name })
		val map = MimeTypeMap.getSingleton()
		return list.firstOrNull {
			map.getMimeTypeFromExtension(it.name.substringAfterLast('.'))
				?.startsWith("image/") == true
		}
	}

	override val sortOrders = emptySet<SortOrder>()

	override suspend fun getPageUrl(page: MangaPage) = page.url

	override suspend fun getTags() = emptySet<MangaTag>()

	suspend fun import(uri: Uri) {
		val contentResolver = storageManager.contentResolver
		withContext(Dispatchers.IO) {
			val name = contentResolver.resolveName(uri)
				?: throw IOException("Cannot fetch name from uri: $uri")
			if (!filenameFilter.isFileSupported(name)) {
				throw UnsupportedFileException("Unsupported file on $uri")
			}
			val dest = File(
				getOutputDir() ?: throw IOException("External files dir unavailable"),
				name,
			)
			runInterruptible {
				contentResolver.openInputStream(uri)?.use { source ->
					dest.outputStream().use { output ->
						source.copyTo(output)
					}
				}
			} ?: throw IOException("Cannot open input stream: $uri")
		}
	}

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

	suspend fun unlockManga(id: Long) {
		locks.unlock(id)
	}

	private suspend fun getAllFiles() = storageManager.getReadableDirs().flatMap { dir ->
		dir.listFiles(filenameFilter)?.toList().orEmpty()
	}

	private fun Manga.copy2(
		url: String = this.url,
		coverUrl: String = this.coverUrl,
		chapters: List<MangaChapter>? = this.chapters,
		source: MangaSource = this.source,
	) = Manga(
		id = id,
		title = title,
		altTitle = altTitle,
		url = url,
		publicUrl = publicUrl,
		rating = rating,
		isNsfw = isNsfw,
		coverUrl = coverUrl,
		tags = tags,
		state = state,
		author = author,
		largeCoverUrl = largeCoverUrl,
		description = description,
		chapters = chapters,
		source = source,
	)

	private fun MangaChapter.copy(
		url: String = this.url,
		source: MangaSource = this.source,
	) = MangaChapter(
		id = id,
		name = name,
		number = number,
		url = url,
		scanlator = scanlator,
		uploadDate = uploadDate,
		branch = branch,
		source = source,
	)
}