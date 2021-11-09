package org.koitharu.kotatsu.local.domain

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.collection.ArraySet
import androidx.core.net.toFile
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koitharu.kotatsu.core.model.*
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.local.data.CbzFilter
import org.koitharu.kotatsu.local.data.MangaIndex
import org.koitharu.kotatsu.local.data.MangaZip
import org.koitharu.kotatsu.utils.AlphanumComparator
import org.koitharu.kotatsu.utils.ext.*
import java.io.File
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

class LocalMangaRepository(private val context: Context) : MangaRepository {

	private val filenameFilter = CbzFilter()

	override suspend fun getList2(
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

	override suspend fun getDetails(manga: Manga) = if (manga.chapters == null) {
		getFromFile(Uri.parse(manga.url).toFile())
	} else manga

	@Suppress("BlockingMethodInNonBlockingContext")
	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
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
		return entries
			.toList()
			.sortedWith(compareBy(AlphanumComparator()) { x -> x.name })
			.map { x ->
				val entryUri = zipUri(file, x.name)
				MangaPage(
					id = entryUri.longHashCode(),
					url = entryUri,
					referer = chapter.url,
					source = MangaSource.LOCAL
				)
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
			return info.copy(
				source = MangaSource.LOCAL,
				url = fileUri,
				coverUrl = zipUri(
					file,
					entryName = index.getCoverEntry()
						?: findFirstEntry(zip.entries(), isImage = true)?.name.orEmpty()
				),
				chapters = info.chapters?.map { c ->
					c.copy(url = fileUri,
						source = MangaSource.LOCAL)
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
			}
		)
	}

	suspend fun getRemoteManga(localManga: Manga): Manga? {
		val file = runCatching {
			Uri.parse(localManga.url).toFile()
		}.getOrNull() ?: return null
		return withContext(Dispatchers.IO) {
			@Suppress("BlockingMethodInNonBlockingContext")
			ZipFile(file).use { zip ->
				val entry = zip.getEntry(MangaZip.INDEX_ENTRY)
				val index = entry?.let(zip::readText)?.let(::MangaIndex) ?: return@withContext null
				index.getMangaInfo()
			}
		}
	}

	suspend fun findSavedManga(remoteManga: Manga): Manga? = withContext(Dispatchers.IO) {
		val files = getAllFiles()
		for (file in files) {
			@Suppress("BlockingMethodInNonBlockingContext")
			val index = ZipFile(file).use { zip ->
				val entry = zip.getEntry(MangaZip.INDEX_ENTRY)
				entry?.let(zip::readText)?.let(::MangaIndex)
			} ?: continue
			val info = index.getMangaInfo() ?: continue
			if (info.id == remoteManga.id) {
				val fileUri = file.toUri().toString()
				return@withContext info.copy(
					source = MangaSource.LOCAL,
					url = fileUri,
					chapters = info.chapters?.map { c -> c.copy(url = fileUri) }
				)
			}
		}
		null
	}

	private fun zipUri(file: File, entryName: String) =
		Uri.fromParts("cbz", file.path, entryName).toString()

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

	private fun getAllFiles() = getAvailableStorageDirs(context).flatMap { dir ->
		dir.listFiles(filenameFilter)?.toList().orEmpty()
	}

	companion object {

		private const val DIR_NAME = "manga"

		fun isFileSupported(name: String): Boolean {
			val ext = name.substringAfterLast('.').lowercase(Locale.ROOT)
			return ext == "cbz" || ext == "zip"
		}

		fun getAvailableStorageDirs(context: Context): List<File> {
			val result = ArrayList<File?>(5)
			result += File(context.filesDir, DIR_NAME)
			result += context.getExternalFilesDirs(DIR_NAME)
			return result.filterNotNull()
				.distinctBy { it.canonicalPath }
				.filter { it.exists() || it.mkdir() }
		}

		fun getFallbackStorageDir(context: Context): File? {
			return context.getExternalFilesDir(DIR_NAME) ?: context.filesDir.sub(DIR_NAME).takeIf {
				(it.exists() || it.mkdir()) && it.canWrite()
			}
		}
	}
}