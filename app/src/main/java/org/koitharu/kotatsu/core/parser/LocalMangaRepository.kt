package org.koitharu.kotatsu.core.parser

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import androidx.core.net.toFile
import androidx.core.net.toUri
import org.koin.core.KoinComponent
import org.koin.core.inject
import org.koitharu.kotatsu.core.local.CbzFilter
import org.koitharu.kotatsu.core.model.*
import org.koitharu.kotatsu.domain.local.MangaIndex
import org.koitharu.kotatsu.domain.local.MangaZip
import org.koitharu.kotatsu.utils.AlphanumComparator
import org.koitharu.kotatsu.utils.ext.longHashCode
import org.koitharu.kotatsu.utils.ext.readText
import org.koitharu.kotatsu.utils.ext.safe
import org.koitharu.kotatsu.utils.ext.sub
import java.io.File
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

class LocalMangaRepository : MangaRepository, KoinComponent {

	private val context by inject<Context>()

	override suspend fun getList(
		offset: Int,
		query: String?,
		sortOrder: SortOrder?,
		tag: MangaTag?
	): List<Manga> {
		val files = getAvailableStorageDirs(context)
			.flatMap { x -> x.listFiles(CbzFilter())?.toList().orEmpty() }
		return files.mapNotNull { x -> safe { getFromFile(x) } }
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
					source = MangaSource.LOCAL
				)
			}
	}


	fun delete(manga: Manga): Boolean {
		val file = Uri.parse(manga.url).toFile()
		return file.delete()
	}

	@SuppressLint("DefaultLocale")
	fun getFromFile(file: File): Manga {
		val zip = ZipFile(file)
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
						?: findFirstEntry(zip.entries())?.name.orEmpty()
				),
				chapters = info.chapters?.map { c -> c.copy(url = fileUri) }
			)
		}
		// fallback
		val title = file.nameWithoutExtension.replace("_", " ").capitalize()
		val chapters = HashSet<String>()
		for (x in zip.entries()) {
			if (!x.isDirectory) {
				chapters += x.name.substringBeforeLast(File.separatorChar, "")
			}
		}
		val uriBuilder = file.toUri().buildUpon()
		return Manga(
			id = file.absolutePath.longHashCode(),
			title = title,
			url = fileUri,
			source = MangaSource.LOCAL,
			coverUrl = zipUri(file, findFirstEntry(zip.entries())?.name.orEmpty()),
			chapters = chapters.sortedWith(AlphanumComparator()).mapIndexed { i, s ->
				MangaChapter(
					id = "$i$s".longHashCode(),
					name = if (s.isEmpty()) title else s,
					number = i + 1,
					source = MangaSource.LOCAL,
					url = uriBuilder.fragment(s).build().toString()
				)
			}
		)
	}

	fun getRemoteManga(localManga: Manga): Manga? {
		val file = safe {
			Uri.parse(localManga.url).toFile()
		} ?: return null
		val zip = ZipFile(file)
		val entry = zip.getEntry(MangaZip.INDEX_ENTRY)
		val index = entry?.let(zip::readText)?.let(::MangaIndex) ?: return null
		return index.getMangaInfo()
	}

	private fun zipUri(file: File, entryName: String) =
		Uri.fromParts("cbz", file.path, entryName).toString()

	private fun findFirstEntry(entries: Enumeration<out ZipEntry>): ZipEntry? {
		val list = entries.toList()
			.filterNot { it.isDirectory }
			.sortedWith(compareBy(AlphanumComparator()) { x -> x.name })
		return list.firstOrNull()
	}

	override val sortOrders = emptySet<SortOrder>()

	override suspend fun getPageFullUrl(page: MangaPage) = page.url

	override suspend fun getTags() = emptySet<MangaTag>()

	companion object {

		private const val DIR_NAME = "manga"

		fun isFileSupported(name: String): Boolean {
			val ext = name.substringAfterLast('.').toLowerCase(Locale.ROOT)
			return ext == "cbz" || ext == "zip"
		}

		fun getAvailableStorageDirs(context: Context): List<File> {
			val result = ArrayList<File>(5)
			result += context.filesDir.sub(DIR_NAME)
			result += context.getExternalFilesDirs(DIR_NAME)
			return result.distinctBy { it.canonicalPath }.filter { it.exists() || it.mkdir() }
		}

		fun getFallbackStorageDir(context: Context): File? {
			return context.getExternalFilesDir(DIR_NAME) ?: context.filesDir.sub(DIR_NAME).takeIf {
				(it.exists() || it.mkdir()) && it.canWrite()
			}
		}
	}
}