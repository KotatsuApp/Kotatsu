package org.koitharu.kotatsu.reader.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.activity.result.ActivityResultLauncher
import androidx.core.net.toFile
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okio.FileSystem
import okio.IOException
import okio.Path.Companion.toPath
import okio.Source
import okio.buffer
import okio.openZip
import okio.sink
import okio.source
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.util.ext.isFileUri
import org.koitharu.kotatsu.core.util.ext.isZipUri
import org.koitharu.kotatsu.core.util.ext.toFileOrNull
import org.koitharu.kotatsu.core.util.ext.writeAllCancellable
import org.koitharu.kotatsu.parsers.model.MangaPage
import org.koitharu.kotatsu.parsers.util.toFileNameSafe
import org.koitharu.kotatsu.reader.domain.PageLoader
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

private const val MAX_FILENAME_LENGTH = 10
private const val EXTENSION_FALLBACK = "png"

class PageSaveHelper @Inject constructor(
	@ApplicationContext private val context: Context,
	private val settings: AppSettings,
) {

	private var continuation: Continuation<Uri>? = null
	private val contentResolver = context.contentResolver

	suspend fun savePage(
		pageLoader: PageLoader,
		page: MangaPage,
		saveLauncher: ActivityResultLauncher<String>,
	): Uri {
		val pageUrl = pageLoader.getPageUrl(page)
		val pageUri = pageLoader.loadPage(page, force = false)
		val proposedName = getProposedFileName(pageUrl, pageUri)
		val destination = getDefaultFileUri(proposedName) ?: pickFileUri(saveLauncher, proposedName)
		runInterruptible(Dispatchers.IO) {
			contentResolver.openOutputStream(destination)?.sink()?.buffer()
		}?.use { output ->
			getSource(pageUri).use { input ->
				output.writeAllCancellable(input)
			}
		} ?: throw IOException("Output stream is null")
		return destination
	}

	private fun getDefaultFileUri(proposedName: String): Uri? {
		if (settings.isPagesSavingAskEnabled) {
			return null
		}
		return settings.getPagesSaveDir(context)?.let {
			val ext = proposedName.substringAfterLast('.', "")
			val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: return null
			it.createFile(mime, proposedName.substringBeforeLast('.'))?.uri
		}
	}

	private fun getSource(uri: Uri): Source = when {
		uri.isFileUri() -> uri.toFile().source()
		uri.isZipUri() -> FileSystem.SYSTEM.openZip(uri.schemeSpecificPart.toPath())
			.source(requireNotNull(uri.fragment).toPath())

		else -> throw IllegalArgumentException("Bad uri $uri: unsupported scheme")
	}

	private suspend fun pickFileUri(saveLauncher: ActivityResultLauncher<String>, proposedName: String): Uri {
		val defaultUri = settings.getPagesSaveDir(context)?.uri?.buildUpon()?.appendPath(proposedName)?.toString()
		return withContext(Dispatchers.Main) {
			suspendCancellableCoroutine { cont ->
				continuation = cont
				saveLauncher.launch(defaultUri ?: proposedName)
			}.also {
				continuation = null
			}
		}
	}

	fun onActivityResult(uri: Uri): Boolean = continuation?.apply {
		resume(uri)
	} != null

	private suspend fun getProposedFileName(url: String, fileUri: Uri): String {
		var name = if (url.startsWith("cbz:")) {
			requireNotNull(url.toUri().fragment)
		} else {
			url.toHttpUrl().pathSegments.last()
		}
		var extension = name.substringAfterLast('.', "")
		name = name.substringBeforeLast('.')
		if (extension.length !in 2..4) {
			val mimeType = fileUri.toFileOrNull()?.let { file -> getImageMimeType(file) }
			extension = if (mimeType != null) {
				MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: EXTENSION_FALLBACK
			} else {
				EXTENSION_FALLBACK
			}
		}
		return name.toFileNameSafe().take(MAX_FILENAME_LENGTH) + "." + extension
	}

	private suspend fun getImageMimeType(file: File): String? = runInterruptible(Dispatchers.IO) {
		val options = BitmapFactory.Options().apply {
			inJustDecodeBounds = true
		}
		BitmapFactory.decodeFile(file.path, options)?.recycle()
		options.outMimeType
	}
}
