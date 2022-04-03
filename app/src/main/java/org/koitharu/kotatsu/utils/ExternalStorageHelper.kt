package org.koitharu.kotatsu.utils

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import okio.IOException
import org.koitharu.kotatsu.parsers.model.MangaPage
import org.koitharu.kotatsu.reader.domain.PageLoader

class ExternalStorageHelper(context: Context) {

	private val contentResolver = context.contentResolver

	suspend fun savePage(page: MangaPage, destination: Uri) {
		val pageLoader = PageLoader()
		val pageFile = pageLoader.loadPage(page, force = false)
		runInterruptible(Dispatchers.IO) {
			contentResolver.openOutputStream(destination)?.use { output ->
				pageFile.inputStream().use { input ->
					input.copyTo(output)
				}
			} ?: throw IOException("Output stream is null")
		}
	}
}