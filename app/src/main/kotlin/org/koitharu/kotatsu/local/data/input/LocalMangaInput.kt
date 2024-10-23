package org.koitharu.kotatsu.local.data.input

import android.net.Uri
import androidx.core.net.toFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import org.koitharu.kotatsu.core.util.ext.toZipUri
import org.koitharu.kotatsu.local.data.hasCbzExtension
import org.koitharu.kotatsu.local.domain.model.LocalManga
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.parsers.model.MangaPage
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
import org.koitharu.kotatsu.parsers.util.toFileNameSafe
import java.io.File

sealed class LocalMangaInput(
	protected val root: File,
) {

	abstract suspend fun getManga(): LocalManga

	abstract suspend fun getMangaInfo(): Manga?

	abstract suspend fun getPages(chapter: MangaChapter): List<MangaPage>

	companion object {

		fun of(manga: Manga): LocalMangaInput = of(Uri.parse(manga.url).toFile())

		fun of(chapter: MangaChapter): LocalMangaInput = of(Uri.parse(chapter.url).toFile())

		fun of(file: File): LocalMangaInput = when {
			file.isDirectory -> LocalMangaDirInput(file)
			else -> LocalMangaZipInput(file)
		}

		fun ofOrNull(file: File): LocalMangaInput? = when {
			file.isDirectory -> LocalMangaDirInput(file)
			hasCbzExtension(file.name) -> LocalMangaZipInput(file)
			else -> null
		}

		suspend fun find(roots: Iterable<File>, manga: Manga): LocalMangaInput? = channelFlow {
			val fileName = manga.title.toFileNameSafe()
			for (root in roots) {
				launch {
					val dir = File(root, fileName)
					val zip = File(root, "$fileName.cbz")
					val input = when {
						dir.isDirectory -> LocalMangaDirInput(dir)
						zip.isFile -> LocalMangaZipInput(zip)
						else -> null
					}
					val info = runCatchingCancellable { input?.getMangaInfo() }.getOrNull()
					if (info?.id == manga.id) {
						send(input)
					}
				}
			}
		}.flowOn(Dispatchers.Default).firstOrNull()

		@JvmStatic
		protected fun zipUri(file: File, entryName: String): String = file.toZipUri(entryName).toString()

		@JvmStatic
		protected fun Manga.copy2(
			url: String,
			coverUrl: String,
			largeCoverUrl: String,
			chapters: List<MangaChapter>?,
			source: MangaSource,
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

		@JvmStatic
		protected fun MangaChapter.copy(
			url: String,
			source: MangaSource,
		) = MangaChapter(
			id = id,
			name = name,
			number = number,
			volume = volume,
			url = url,
			scanlator = scanlator,
			uploadDate = uploadDate,
			branch = branch,
			source = source,
		)
	}
}
