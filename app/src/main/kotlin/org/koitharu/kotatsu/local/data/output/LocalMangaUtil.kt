package org.koitharu.kotatsu.local.data.output

import androidx.core.net.toFile
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import org.koitharu.kotatsu.core.model.isLocal
import org.koitharu.kotatsu.parsers.model.Manga

class LocalMangaUtil(
	private val manga: Manga,
) {

	init {
		require(manga.isLocal) { "Expected LOCAL source but ${manga.source} found" }
	}

	suspend fun deleteChapters(ids: Set<Long>) {
		newOutput().use { output ->
			when (output) {
				is LocalMangaZipOutput -> runInterruptible(Dispatchers.IO) {
					LocalMangaZipOutput.filterChapters(output, ids)
				}

				is LocalMangaDirOutput -> {
					output.deleteChapters(ids)
					output.finish()
				}
			}
		}
	}

	private suspend fun newOutput(): LocalMangaOutput = runInterruptible(Dispatchers.IO) {
		val file = manga.url.toUri().toFile()
		if (file.isDirectory) {
			LocalMangaDirOutput(file, manga)
		} else {
			LocalMangaZipOutput(file, manga)
		}
	}
}
