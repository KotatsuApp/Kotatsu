package org.koitharu.kotatsu.local.data.input

import androidx.core.net.toFile
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import org.koitharu.kotatsu.core.util.AlphanumComparator
import org.koitharu.kotatsu.core.util.ext.creationTime
import org.koitharu.kotatsu.core.util.ext.listFilesRecursive
import org.koitharu.kotatsu.core.util.ext.longHashCode
import org.koitharu.kotatsu.core.util.ext.toListSorted
import org.koitharu.kotatsu.local.data.CbzFilter
import org.koitharu.kotatsu.local.data.ImageFileFilter
import org.koitharu.kotatsu.local.data.MangaIndex
import org.koitharu.kotatsu.local.data.output.LocalMangaOutput
import org.koitharu.kotatsu.local.domain.model.LocalManga
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.parsers.model.MangaPage
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.util.toCamelCase
import java.io.File
import java.util.zip.ZipFile

/**
 * Manga {Folder}
 * |--- index.json (optional)
 * |--- Chapter 1.cbz
 *   |--- Page 1.png
 *   :
 *   L--- Page x.png
 * |--- Chapter 2.cbz
 * :
 * L--- Chapter x.cbz
 */
class LocalMangaDirInput(root: File) : LocalMangaInput(root) {

	override suspend fun getManga(): LocalManga = runInterruptible(Dispatchers.IO) {
		val index = MangaIndex.read(File(root, LocalMangaOutput.ENTRY_NAME_INDEX))
		val mangaUri = root.toUri().toString()
		val chapterFiles = getChaptersFiles()
		val info = index?.getMangaInfo()
		val manga = info?.copy2(
			source = MangaSource.LOCAL,
			url = mangaUri,
			coverUrl = fileUri(
				root,
				index.getCoverEntry() ?: findFirstImageEntry().orEmpty(),
			),
			chapters = info.chapters?.mapIndexed { i, c ->
				c.copy(url = chapterFiles[i].toUri().toString(), source = MangaSource.LOCAL)
			},
		) ?: Manga(
			id = root.absolutePath.longHashCode(),
			title = root.name.toHumanReadable(),
			url = mangaUri,
			publicUrl = mangaUri,
			source = MangaSource.LOCAL,
			coverUrl = findFirstImageEntry().orEmpty(),
			chapters = chapterFiles.mapIndexed { i, f ->
				MangaChapter(
					id = "$i${f.name}".longHashCode(),
					name = f.nameWithoutExtension.toHumanReadable(),
					number = i + 1,
					source = MangaSource.LOCAL,
					uploadDate = f.creationTime,
					url = f.toUri().toString(),
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
		LocalManga(manga, root)
	}

	override suspend fun getMangaInfo(): Manga? = runInterruptible(Dispatchers.IO) {
		val index = MangaIndex.read(File(root, LocalMangaOutput.ENTRY_NAME_INDEX))
		index?.getMangaInfo()
	}

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> = runInterruptible(Dispatchers.IO) {
		val file = chapter.url.toUri().toFile()
		if (file.isDirectory) {
			file.listFilesRecursive(ImageFileFilter())
				.toListSorted(compareBy(AlphanumComparator()) { x -> x.name })
				.map {
					val pageUri = it.toUri().toString()
					MangaPage(
						id = pageUri.longHashCode(),
						url = pageUri,
						preview = null,
						source = MangaSource.LOCAL,
					)
				}
		} else {
			ZipFile(file).use { zip ->
				zip.entries()
					.asSequence()
					.filter { x -> !x.isDirectory }
					.map { it.name }
					.toListSorted(AlphanumComparator())
					.map {
						val pageUri = zipUri(file, it)
						MangaPage(
							id = pageUri.longHashCode(),
							url = pageUri,
							preview = null,
							source = MangaSource.LOCAL,
						)
					}
			}
		}
	}

	private fun String.toHumanReadable() = replace("_", " ").toCamelCase()

	private fun getChaptersFiles(): List<File> = root.listFilesRecursive(CbzFilter())
		.toListSorted(compareBy(AlphanumComparator()) { x -> x.name })

	private fun findFirstImageEntry(): String? {
		val filter = ImageFileFilter()
		root.listFilesRecursive(filter).firstOrNull()?.let {
			return it.toUri().toString()
		}
		val cbz = root.listFilesRecursive(CbzFilter()).firstOrNull() ?: return null
		return ZipFile(cbz).use { zip ->
			zip.entries().asSequence()
				.firstOrNull { x -> !x.isDirectory && filter.accept(x) }
				?.let { entry -> zipUri(cbz, entry.name) }
		}
	}

	private fun fileUri(base: File, name: String): String {
		return File(base, name).toUri().toString()
	}
}
