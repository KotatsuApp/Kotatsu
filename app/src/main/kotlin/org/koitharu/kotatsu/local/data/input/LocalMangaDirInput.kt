package org.koitharu.kotatsu.local.data.input

import androidx.core.net.toFile
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import org.koitharu.kotatsu.core.util.AlphanumComparator
import org.koitharu.kotatsu.core.util.ext.longHashCode
import org.koitharu.kotatsu.core.util.ext.toListSorted
import org.koitharu.kotatsu.local.data.MangaIndex
import org.koitharu.kotatsu.local.data.hasImageExtension
import org.koitharu.kotatsu.local.data.isCbzExtension
import org.koitharu.kotatsu.local.data.isImageExtension
import org.koitharu.kotatsu.local.data.output.LocalMangaOutput
import org.koitharu.kotatsu.local.domain.model.LocalManga
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.parsers.model.MangaPage
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.util.toCamelCase
import java.io.File
import java.nio.file.Path
import java.util.zip.ZipFile
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.extension
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.walk

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
					uploadDate = f.getLastModifiedTime().toMillis(),
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
		LocalManga(root, manga)
	}

	override suspend fun getMangaInfo(): Manga? = runInterruptible(Dispatchers.IO) {
		val index = MangaIndex.read(File(root, LocalMangaOutput.ENTRY_NAME_INDEX))
		index?.getMangaInfo()
	}

	@OptIn(ExperimentalPathApi::class)
	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> = runInterruptible(Dispatchers.IO) {
		val file = chapter.url.toUri().toFile()
		if (file.isDirectory) {
			file.toPath().walk()
				.filter { isImageExtension(it.extension) }
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

	@OptIn(ExperimentalPathApi::class)
	private fun getChaptersFiles(): List<Path> = root.toPath().walk()
		.filter { isCbzExtension(it.extension) }
		.toListSorted(compareBy(AlphanumComparator()) { x -> x.name })

	@OptIn(ExperimentalPathApi::class)
	private fun findFirstImageEntry(): String? {
		val rootPath = root.toPath()
		return rootPath.walk()
			.filter { isImageExtension(it.extension) }
			.firstOrNull()?.toUri()?.toString()
			?: run {
				val cbz = rootPath.walk().filter { isCbzExtension(it.extension) }.firstOrNull()?.toFile() ?: return null
				return ZipFile(cbz).use { zip ->
					zip.entries().asSequence()
						.firstOrNull { x -> !x.isDirectory && hasImageExtension(x) }
						?.let { entry -> zipUri(cbz, entry.name) }
				}
			}
	}

	private fun fileUri(base: File, name: String): String {
		return File(base, name).toUri().toString()
	}
}
