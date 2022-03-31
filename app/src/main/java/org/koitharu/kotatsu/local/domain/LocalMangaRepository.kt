package org.koitharu.kotatsu.local.domain

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.collection.ArraySet
import androidx.core.net.toFile
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import org.koitharu.kotatsu.core.exceptions.UnsupportedFileException
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.local.data.CbzFilter
import org.koitharu.kotatsu.local.data.LocalStorageManager
import org.koitharu.kotatsu.local.data.MangaIndex
import org.koitharu.kotatsu.local.data.MangaZip
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.util.longHashCode
import org.koitharu.kotatsu.parsers.util.toCamelCase
import org.koitharu.kotatsu.utils.AlphanumComparator
import org.koitharu.kotatsu.utils.ext.deleteAwait
import org.koitharu.kotatsu.utils.ext.readText
import org.koitharu.kotatsu.utils.ext.resolveName
import java.io.File
import java.io.IOException
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

class LocalMangaRepository(private val storageManager: LocalStorageManager) : MangaRepository {

	override val source = MangaSource.LOCAL
	private val filenameFilter = CbzFilter()

	override suspend fun getList(
		offset: Int,
		query: String?,
		tags: Set<MangaTag>?,
		sortOrder: SortOrder?
	): List<Manga> {
		require(offset == 0) {
			"LocalMangaRepository does not support pagination"
		}
		val files = getAllFiles()
		return files.mapNotNull { x -> runCatching { getFromFile(x) }.getOrNull() }
	}

	override suspend fun getDetails(manga: Manga) = when {
		manga.source != MangaSource.LOCAL -> requireNotNull(findSavedManga(manga)) {
			"Manga is not local or saved"
		}
		manga.chapters == null -> getFromFile(Uri.parse(manga.url).toFile())
		else -> manga
	}

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		return runInterruptible(Dispatchers.IO){
			val uri = Uri.parse(chapter.url)
			val file = uri.toFile()
			val zip = ZipFile(file)
			val index = zip.getEntry(MangaZip.INDEX_ENTRY)?.let(zip::readText)?.let(::MangaIndex)
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

	@SuppressLint("DefaultLocale")
	fun getFromFile(file: File): Manga = ZipFile(file).use { zip ->
		val fileUri = file.toUri().toString()
		val entry = zip.getEntry(MangaZip.INDEX_ENTRY)
		val index = entry?.let(zip::readText)?.let(::MangaIndex)
		val info = index?.getMangaInfo()
		if (index != null && info != null) {
			return info.copy2(
				source = MangaSource.LOCAL,
				url = fileUri,
				coverUrl = zipUri(
					file,
					entryName = index.getCoverEntry()
						?: findFirstEntry(zip.entries(), isImage = true)?.name.orEmpty()
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
			coverUrl = zipUri(file, findFirstEntry(zip.entries(), isImage = true)?.name.orEmpty()),
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
				val entry = zip.getEntry(MangaZip.INDEX_ENTRY)
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
					val entry = zip.getEntry(MangaZip.INDEX_ENTRY)
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

	private fun zipUri(file: File, entryName: String) = Uri.fromParts("cbz", file.path, entryName).toString()

	private fun findFirstEntry(entries: Enumeration<out ZipEntry>, isImage: Boolean): ZipEntry? {
		val list = entries.toList()
			.filterNot { it.isDirectory }
			.sortedWith(compareBy(AlphanumComparator()) { x -> x.name })
		return if (isImage) {
			val map = MimeTypeMap.getSingleton()
			list.firstOrNull {
				map.getMimeTypeFromExtension(it.name.substringAfterLast('.'))
					?.startsWith("image/") == true
			}
		} else {
			list.firstOrNull()
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
			if (!isFileSupported(name)) {
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

	fun isFileSupported(name: String): Boolean {
		val ext = name.substringAfterLast('.').lowercase(Locale.ROOT)
		return ext == "cbz" || ext == "zip"
	}

	suspend fun getOutputDir(): File? {
		return storageManager.getDefaultWriteableDir()
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