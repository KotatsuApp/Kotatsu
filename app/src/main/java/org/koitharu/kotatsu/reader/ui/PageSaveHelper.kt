package org.koitharu.kotatsu.reader.ui

import android.content.Context
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.HttpUrl.Companion.toHttpUrl
import okio.IOException
import org.koitharu.kotatsu.local.data.PagesCache
import org.koitharu.kotatsu.parsers.model.MangaPage
import org.koitharu.kotatsu.reader.domain.PageLoader
import kotlin.coroutines.Continuation
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume

class PageSaveHelper(
	private val cache: PagesCache,
	context: Context,
) {

	private var continuation: Continuation<Uri>? = null
	private val contentResolver = context.contentResolver

	suspend fun savePage(
		pageLoader: PageLoader,
		page: MangaPage,
		saveLauncher: ActivityResultLauncher<String>,
	): Uri {
		var pageFile = cache[page.url]
		var fileName = pageFile?.name
		if (fileName == null) {
			fileName = pageLoader.getPageUrl(page).toHttpUrl().pathSegments.last()
		}
		val cc = coroutineContext
		val destination = suspendCancellableCoroutine<Uri> { cont ->
			continuation = cont
			Dispatchers.Main.dispatch(cc) {
				saveLauncher.launch(fileName)
			}
		}
		continuation = null
		if (pageFile == null) {
			pageFile = pageLoader.loadPage(page, force = false)
		}
		runInterruptible(Dispatchers.IO) {
			contentResolver.openOutputStream(destination)?.use { output ->
				pageFile.inputStream().use { input ->
					input.copyTo(output)
				}
			} ?: throw IOException("Output stream is null")
		}
		return destination
	}

	fun onActivityResult(uri: Uri): Boolean = continuation?.apply {
		resume(uri)
	} != null
}