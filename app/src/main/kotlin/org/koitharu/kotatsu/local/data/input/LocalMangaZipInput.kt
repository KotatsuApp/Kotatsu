package org.koitharu.kotatsu.local.data.input

import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.collection.ArraySet
import androidx.core.net.toFile
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import org.koitharu.kotatsu.core.model.LocalMangaSource
import org.koitharu.kotatsu.core.util.AlphanumComparator
import org.koitharu.kotatsu.core.util.ext.longHashCode
import org.koitharu.kotatsu.core.util.ext.readText
import org.koitharu.kotatsu.core.util.ext.toListSorted
import org.koitharu.kotatsu.local.data.MangaIndex
import org.koitharu.kotatsu.local.data.output.LocalMangaOutput
import org.koitharu.kotatsu.local.domain.model.LocalManga
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.parsers.model.MangaPage
import org.koitharu.kotatsu.parsers.util.toCamelCase
import java.io.File
import java.util.Enumeration
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * Manga archive {.cbz or .zip file}
 * |--- index.json (optional)
 * |--- Page 1.png
 * |--- Page 2.png
 * :
 * L--- Page x.png
 */
class LocalMangaZipInput(root: File) : LocalMangaInput(root) {

	override suspend fun getManga(): LocalManga {
		val manga = runInterruptible(Dispatchers.IO) {
			ZipFile(root).use { zip ->
				val fileUri = root.toUri().toString()
				val entry = zip.getEntry(LocalMangaOutput.ENTRY_NAME_INDEX)
				val index = entry?.let(zip::readText)?.let(::MangaIndex)
				val info = index?.getMangaInfo()
				if (info != null) {
					val cover = zipUri(
						root,
						entryName = index.getCoverEntry() ?: findFirstImageEntry(zip.entries())?.name.orEmpty(),
					)
					return@use info.copy2(
						source = LocalMangaSource,
						url = fileUri,
						coverUrl = cover,
						largeCoverUrl = cover,
						chapters = info.chapters?.map { c ->
							c.copy(url = fileUri, source = LocalMangaSource)
						},
					)
				}
				// fallback
				val title = root.nameWithoutExtension.replace("_", " ").toCamelCase()
				val chapters = ArraySet<String>()
				for (x in zip.entries()) {
					if (!x.isDirectory) {
						chapters += x.name.substringBeforeLast(File.separatorChar, "")
					}
				}
				val uriBuilder = root.toUri().buildUpon()
				Manga(
					id = root.absolutePath.longHashCode(),
					title = title,
					url = fileUri,
					publicUrl = fileUri,
					source = LocalMangaSource,
					coverUrl = zipUri(root, findFirstImageEntry(zip.entries())?.name.orEmpty()),
					chapters = chapters.sortedWith(AlphanumComparator())
						.mapIndexed { i, s ->
							MangaChapter(
								id = "$i$s".longHashCode(),
								name = s.ifEmpty { title },
								number = 0f,
								volume = 0,
								source = LocalMangaSource,
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
		}
		return LocalManga(manga, root)
	}

	override suspend fun getMangaInfo(): Manga? = runInterruptible(Dispatchers.IO) {
		ZipFile(root).use { zip ->
			val entry = zip.getEntry(LocalMangaOutput.ENTRY_NAME_INDEX)
			val index = entry?.let(zip::readText)?.let(::MangaIndex)
			index?.getMangaInfo()
		}
	}

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		return runInterruptible(Dispatchers.IO) {
			val uri = Uri.parse(chapter.url)
			val file = uri.toFile()
			val zip = ZipFile(file)
			val index = zip.getEntry(LocalMangaOutput.ENTRY_NAME_INDEX)?.let(zip::readText)?.let(::MangaIndex)
			var entries = zip.entries().asSequence()
			entries = if (index != null) {
				val pattern = index.getChapterNamesPattern(chapter)
				entries.filter { x -> !x.isDirectory && x.name.substringBefore('.').matches(pattern) }
			} else {
				val parent = uri.fragment.orEmpty()
				entries.filter { x ->
					!x.isDirectory && x.name.substringBeforeLast(
						File.separatorChar,
						"",
					) == parent
				}
			}
			entries
				.toListSorted(compareBy(AlphanumComparator()) { x -> x.name })
				.map { x ->
					val entryUri = zipUri(file, x.name)
					MangaPage(
						id = entryUri.longHashCode(),
						url = entryUri,
						preview = null,
						source = LocalMangaSource,
					)
				}
		}
	}

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
}
