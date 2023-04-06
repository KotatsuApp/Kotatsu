package org.koitharu.kotatsu.local.data.input

import android.net.Uri
import androidx.core.net.toFile
import org.koitharu.kotatsu.local.data.LocalManga
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.parsers.model.MangaPage
import org.koitharu.kotatsu.parsers.model.MangaSource
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

		@JvmStatic
		protected fun zipUri(file: File, entryName: String) = "cbz://${file.path}#$entryName"

		@JvmStatic
		protected fun Manga.copy2(
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

		@JvmStatic
		protected fun MangaChapter.copy(
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
}
