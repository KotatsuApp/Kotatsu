package org.koitharu.kotatsu.reader.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
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
import javax.inject.Provider
import kotlin.coroutines.resume

class PageSaveHelper @AssistedInject constructor(
	@Assisted activityResultCaller: ActivityResultCaller,
	@ApplicationContext private val context: Context,
	private val settings: AppSettings,
	private val pageLoaderProvider: Provider<PageLoader>,
) : ActivityResultCallback<Uri?> {

	private val savePageRequest = activityResultCaller.registerForActivityResult(PageSaveContract(), this)
	private val pickDirectoryRequest =
		activityResultCaller.registerForActivityResult(ActivityResultContracts.OpenDocumentTree(), this)

	private var continuation: CancellableContinuation<Uri>? = null

	override fun onActivityResult(result: Uri?) {
		continuation?.also { cont ->
			if (result != null) {
				cont.resume(result)
			} else {
				cont.cancel()
			}
		}
	}

	suspend fun save(pages: Collection<MangaPage>): Uri? = when (pages.size) {
		0 -> null
		1 -> saveImpl(pages.first())
		else -> {
			saveImpl(pages)
			null
		}
	}

	private suspend fun saveImpl(page: MangaPage): Uri {
		val pageLoader = pageLoaderProvider.get()
		val pageUrl = pageLoader.getPageUrl(page).toUri()
		val pageUri = pageLoader.loadPage(page, force = false)
		val proposedName = getProposedFileName(pageUrl, pageUri)
		val destination = getDefaultFileUri(proposedName)?.uri ?: run {
			val defaultUri = settings.getPagesSaveDir(context)?.uri?.buildUpon()?.appendPath(proposedName)?.toString()
			savePageRequest.launchAndAwait(defaultUri ?: proposedName)
		}
		copyImpl(pageUri, destination)
		return destination
	}

	private suspend fun saveImpl(pages: Collection<MangaPage>) {
		val pageLoader = pageLoaderProvider.get()
		val destinationDir = getDefaultFileUri(null) ?: run {
			val defaultUri = settings.getPagesSaveDir(context)?.uri
			DocumentFile.fromTreeUri(context, pickDirectoryRequest.launchAndAwait(defaultUri))
		} ?: throw IOException("Cannot get destination directory")
		for (page in pages) {
			val pageUrl = pageLoader.getPageUrl(page).toUri()
			val pageUri = pageLoader.loadPage(page, force = false)
			val proposedName = getProposedFileName(pageUrl, pageUri)
			val ext = proposedName.substringAfterLast('.', "")
			val mime = requireNotNull(MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)) {
				"Unknown type of $proposedName"
			}
			val destination = destinationDir.createFile(mime, proposedName.substringBeforeLast('.'))
			copyImpl(pageUri, destination?.uri ?: throw IOException("Cannot create destination file"))
		}
	}

	private suspend fun <I> ActivityResultLauncher<I>.launchAndAwait(input: I): Uri {
		continuation?.cancel()
		return withContext(Dispatchers.Main) {
			try {
				suspendCancellableCoroutine { cont ->
					continuation = cont
					launch(input)
				}
			} finally {
				continuation = null
			}
		}
	}

	private fun getDefaultFileUri(proposedName: String?): DocumentFile? {
		if (settings.isPagesSavingAskEnabled) {
			return null
		}
		val dir = settings.getPagesSaveDir(context) ?: return null
		if (proposedName == null) {
			return dir
		} else {
			val ext = proposedName.substringAfterLast('.', "")
			val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: return null
			return dir.createFile(mime, proposedName.substringBeforeLast('.'))
		}
	}

	private fun getSource(uri: Uri): Source = when {
		uri.isFileUri() -> uri.toFile().source()
		uri.isZipUri() -> FileSystem.SYSTEM.openZip(uri.schemeSpecificPart.toPath())
			.source(requireNotNull(uri.fragment).toPath())

		else -> throw IllegalArgumentException("Bad uri $uri: unsupported scheme")
	}

	private suspend fun copyImpl(source: Uri, destination: Uri) = withContext(Dispatchers.IO) {
		runInterruptible {
			context.contentResolver.openOutputStream(destination) ?: throw IOException("Output stream is null")
		}.sink().buffer().use { sink ->
			getSource(source).use { input ->
				sink.writeAllCancellable(input)
			}
		}
	}

	private suspend fun getProposedFileName(url: Uri, fileUri: Uri): String {
		var name = requireNotNull(
			if (url.isZipUri()) {
				url.fragment?.substringAfterLast(File.separatorChar)
			} else {
				url.lastPathSegment
			},
		) { "Invalid page url: $url" }
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

	@AssistedFactory
	interface Factory {

		fun create(activityResultCaller: ActivityResultCaller): PageSaveHelper
	}

	private companion object {

		private const val MAX_FILENAME_LENGTH = 16
		private const val EXTENSION_FALLBACK = "png"
	}
}
