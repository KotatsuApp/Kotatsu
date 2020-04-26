package org.koitharu.kotatsu.core.parser

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
		val file = Uri.parse(chapter.url).toFile()
		val zip = ZipFile(file)
		val pattern = zip.getEntry(MangaZip.INDEX_ENTRY)?.let(zip::readText)?.let(::MangaIndex)
			?.getChapterNamesPattern(chapter)
		val entries = if (pattern != null) {
			zip.entries().asSequence()
				.filter { x -> !x.isDirectory && x.name.substringBefore('.').matches(pattern) }
		} else {
			zip.entries().asSequence().filter { x -> !x.isDirectory }
		}.toList().sortedWith(compareBy(AlphanumComparator()) { x -> x.name })
		return entries.map { x ->
			val uri = zipUri(file, x.name)
			MangaPage(
				id = uri.longHashCode(),
				url = uri,
				source = MangaSource.LOCAL
			)
		}
	}


	fun delete(manga: Manga): Boolean {
		val file = Uri.parse(manga.url).toFile()
		return file.delete()
	}

	fun getFromFile(file: File): Manga {
		val zip = ZipFile(file)
		val fileUri = file.toUri().toString()
		val entry = zip.getEntry(MangaZip.INDEX_ENTRY)
		val index = entry?.let(zip::readText)?.let(::MangaIndex)
		return index?.let {
			it.getMangaInfo()?.let { x ->
				x.copy(
					source = MangaSource.LOCAL,
					url = fileUri,
					coverUrl = zipUri(
						file,
						entryName = it.getCoverEntry()
							?: findFirstEntry(zip.entries())?.name.orEmpty()
					),
					chapters = x.chapters?.map { c -> c.copy(url = fileUri) }
				)
			}
		} ?: run {
			val title = file.nameWithoutExtension.replace("_", " ").capitalize()
			Manga(
				id = file.absolutePath.longHashCode(),
				title = title,
				url = fileUri,
				source = MangaSource.LOCAL,
				coverUrl = zipUri(file, findFirstEntry(zip.entries())?.name.orEmpty()),
				chapters = listOf(
					MangaChapter(
						id = file.absolutePath.longHashCode(),
						url = fileUri,
						number = 1,
						source = MangaSource.LOCAL,
						name = title
					)
				)
			)
		}
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
	}
}