package org.koitharu.kotatsu.core.parser

import android.content.Context
import android.net.Uri
import androidx.core.net.toFile
import androidx.core.net.toUri
import org.koin.core.inject
import org.koitharu.kotatsu.core.local.CbzFilter
import org.koitharu.kotatsu.core.model.*
import org.koitharu.kotatsu.domain.MangaLoaderContext
import org.koitharu.kotatsu.domain.local.MangaIndex
import org.koitharu.kotatsu.domain.local.MangaZip
import org.koitharu.kotatsu.utils.ext.longHashCode
import org.koitharu.kotatsu.utils.ext.readText
import org.koitharu.kotatsu.utils.ext.safe
import java.io.File
import java.util.zip.ZipFile

class LocalMangaRepository(loaderContext: MangaLoaderContext) : BaseMangaRepository(loaderContext) {

	private val context by loaderContext.inject<Context>()

	override suspend fun getList(
		offset: Int,
		query: String?,
		sortOrder: SortOrder?,
		tag: MangaTag?
	): List<Manga> {
		val files = context.getExternalFilesDirs("manga")
			.flatMap { x -> x?.listFiles(CbzFilter())?.toList().orEmpty() }
		return files.mapNotNull { x -> safe { getDetails(x) } }
	}

	override suspend fun getDetails(manga: Manga) = manga

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
		}
		return entries.map { x ->
			val uri = zipUri(file, x.name)
			MangaPage(
				id = uri.longHashCode(),
				url = uri,
				source = MangaSource.LOCAL
			)
		}.toList()
	}

	private fun getDetails(file: File): Manga {
		val zip = ZipFile(file)
		val fileUri = file.toUri().toString()
		val entry = zip.getEntry(MangaZip.INDEX_ENTRY)
		val index = entry?.let(zip::readText)?.let(::MangaIndex)
		return index?.let {
			it.getMangaInfo()?.let { x ->
				x.copy(
					source = MangaSource.LOCAL,
					url = fileUri,
					coverUrl = zipUri(file, it.getCoverEntry() ?: zip.entries().nextElement().name),
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
				coverUrl = zipUri(file, zip.entries().nextElement().name),
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

	private fun zipUri(file: File, entryName: String) =
		Uri.fromParts("zip", file.path, entryName).toString()
}