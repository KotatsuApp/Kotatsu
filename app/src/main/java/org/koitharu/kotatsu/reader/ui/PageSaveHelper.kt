package org.koitharu.kotatsu.reader.ui

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.activity.result.ActivityResultLauncher
import androidx.core.net.toUri
import java.io.File
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okio.IOException
import org.koitharu.kotatsu.base.domain.MangaUtils
import org.koitharu.kotatsu.parsers.model.MangaPage
import org.koitharu.kotatsu.parsers.util.toFileNameSafe
import org.koitharu.kotatsu.reader.domain.PageLoader

private const val MAX_FILENAME_LENGTH = 10
private const val EXTENSION_FALLBACK = "png"

class PageSaveHelper(
	context: Context,
) {

	private var continuation: Continuation<Uri>? = null
	private val contentResolver = context.contentResolver

	suspend fun savePage(
		pageLoader: PageLoader,
		page: MangaPage,
		saveLauncher: ActivityResultLauncher<String>,
	): Uri {
		val pageUrl = pageLoader.getPageUrl(page)
		val pageFile = pageLoader.loadPage(page, force = false)
		val proposedName = getProposedFileName(pageUrl, pageFile)
		val destination = withContext(Dispatchers.Main) {
			suspendCancellableCoroutine<Uri> { cont ->
				continuation = cont
				saveLauncher.launch(proposedName)
			}.also {
				continuation = null
			}
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

	private suspend fun getProposedFileName(url: String, file: File): String {
		var name = if (url.startsWith("cbz://")) {
			requireNotNull(url.toUri().fragment)
		} else {
			url.toHttpUrl().pathSegments.last()
		}
		var extension = name.substringAfterLast('.', "")
		name = name.substringBeforeLast('.')
		if (extension.length !in 2..4) {
			val mimeType = MangaUtils.getImageMimeType(file)
			extension = if (mimeType != null) {
				MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: EXTENSION_FALLBACK
			} else {
				EXTENSION_FALLBACK
			}
		}
		return name.toFileNameSafe().take(MAX_FILENAME_LENGTH) + "." + extension
	}
}