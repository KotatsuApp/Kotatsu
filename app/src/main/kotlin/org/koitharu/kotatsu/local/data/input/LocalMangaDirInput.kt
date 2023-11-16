package org.koitharu.kotatsu.local.data.input

import androidx.core.net.toFile
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import org.koitharu.kotatsu.core.util.AlphanumComparator
import org.koitharu.kotatsu.core.util.ext.creationTime
import org.koitharu.kotatsu.core.util.ext.longHashCode
import org.koitharu.kotatsu.core.util.ext.toListSorted
import org.koitharu.kotatsu.core.util.ext.walkCompat
import org.koitharu.kotatsu.local.data.MangaIndex
import org.koitharu.kotatsu.local.data.hasCbzExtension
import org.koitharu.kotatsu.local.data.hasImageExtension
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
		val cover = fileUri(
			root,
			index?.getCoverEntry() ?: findFirstImageEntry().orEmpty(),
		)
		val manga = info?.copy2(
			source = MangaSource.LOCAL,
			url = mangaUri,
			coverUrl = cover,
			largeCoverUrl = cover,
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
			file.walkCompat()
				.filter { hasImageExtension(it) }
				.toListSorted(compareBy(AlphanumComparator()) { x -> x.name })
				.map {
					val pageUri = it.toUri().toString()
					MangaPage(pageUri.longHashCode(), pageUri, null, MangaSource.LOCAL)
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

	private fun getChaptersFiles(): List<File> = root.walkCompat()
		.filter { hasCbzExtension(it) }
		.toListSorted(compareBy(AlphanumComparator()) { it.name })

	private fun findFirstImageEntry(): String? {
		return root.walkCompat().firstOrNull { hasImageExtension(it) }?.toUri()?.toString()
			?: run {
				val cbz = root.walkCompat().firstOrNull { hasCbzExtension(it) } ?: return null
				ZipFile(cbz).use { zip ->
					zip.entries().asSequence()
						.firstOrNull { !it.isDirectory && hasImageExtension(it.name) }
						?.let { zipUri(cbz, it.name) }
				}
			}
	}

	private fun fileUri(base: File, name: String): String {
		return File(base, name).toUri().toString()
	}
}
