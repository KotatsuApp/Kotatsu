package org.koitharu.kotatsu.reader.ui

import android.content.Context
import android.net.Uri
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
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
import org.koitharu.kotatsu.core.LocalizedAppContext
import org.koitharu.kotatsu.core.image.BitmapDecoderCompat
import org.koitharu.kotatsu.core.os.OpenDocumentTreeHelper
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.util.MimeTypes
import org.koitharu.kotatsu.core.util.ext.isFileUri
import org.koitharu.kotatsu.core.util.ext.isZipUri
import org.koitharu.kotatsu.core.util.ext.toFileNameSafe
import org.koitharu.kotatsu.core.util.ext.toFileOrNull
import org.koitharu.kotatsu.core.util.ext.writeAllCancellable
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaPage
import org.koitharu.kotatsu.reader.domain.PageLoader
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import javax.inject.Provider
import kotlin.coroutines.resume

class PageSaveHelper @AssistedInject constructor(
	@Assisted activityResultCaller: ActivityResultCaller,
	@LocalizedAppContext private val context: Context,
	private val settings: AppSettings,
	private val pageLoaderProvider: Provider<PageLoader>,
) : ActivityResultCallback<Uri?> {

	private val savePageRequest = activityResultCaller.registerForActivityResult(PageSaveContract(), this)
	private val pickDirectoryRequest = OpenDocumentTreeHelper(activityResultCaller, this)

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

	suspend fun save(tasks: Collection<Task>): Collection<Uri> = when (tasks.size) {
		0 -> emptySet()
		1 -> setOf(saveImpl(tasks.first()))
		else -> saveImpl(tasks)
	}

	suspend fun saveToTempFile(task: Task): File {
		val pageLoader = getPageLoader()
		val pageUrl = pageLoader.getPageUrl(task.page).toUri()
		val pageUri = pageLoader.loadPage(task.page, force = false)
		val proposedName = task.getFileBaseName() + "." + getPageExtension(pageUrl, pageUri)
		val destination = File(checkNotNull(context.getExternalFilesDir(TEMP_DIR)), proposedName)
		copyImpl(pageUri, destination.toUri())
		return destination
	}

	private suspend fun saveImpl(task: Task): Uri {
		val pageLoader = getPageLoader()
		val pageUrl = pageLoader.getPageUrl(task.page).toUri()
		val pageUri = pageLoader.loadPage(task.page, force = false)
		val proposedName = task.getFileBaseName() + "." + getPageExtension(pageUrl, pageUri)
		val destination = getDefaultFileUri(proposedName)?.uri ?: run {
			val defaultUri = settings.getPagesSaveDir(context)?.uri?.buildUpon()?.appendPath(proposedName)?.toString()
			savePageRequest.launchAndAwait(defaultUri ?: proposedName)
		}
		copyImpl(pageUri, destination)
		return destination
	}

	private suspend fun saveImpl(tasks: Collection<Task>): Collection<Uri> {
		val pageLoader = getPageLoader()
		val destinationDir = getDefaultFileUri(null) ?: run {
			val defaultUri = settings.getPagesSaveDir(context)?.uri
			DocumentFile.fromTreeUri(context, pickDirectoryRequest.launchAndAwait(defaultUri))
		} ?: throw IOException("Cannot get destination directory")

		val result = ArrayList<Uri>(tasks.size)
		for (task in tasks) {
			val pageUrl = pageLoader.getPageUrl(task.page).toUri()
			val pageUri = pageLoader.loadPage(task.page, force = false)
			val proposedName = task.getFileBaseName()
			val ext = getPageExtension(pageUrl, pageUri)
			val mime = requireNotNull(MimeTypes.getMimeTypeFromExtension("_.$ext")) {
				"Unknown type of $proposedName"
			}
			val destination = destinationDir.createFile(mime.toString(), proposedName)
			copyImpl(pageUri, destination?.uri ?: throw IOException("Cannot create destination file"))
			result.add(destination.uri)
		}
		return result
	}

	private suspend fun getPageExtension(url: Uri, fileUri: Uri): String {
		val name = requireNotNull(
			if (url.isZipUri()) {
				url.fragment?.substringAfterLast(File.separatorChar)
			} else {
				url.lastPathSegment
			},
		) { "Invalid page url: $url" }
		var extension = name.substringAfterLast('.', "")
		if (extension.length !in 2..4) {
			extension = fileUri.toFileOrNull()?.let { file -> getImageExtension(file) } ?: EXTENSION_FALLBACK
		}
		return extension
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

	private suspend fun getPageLoader() = withContext(Dispatchers.Main.immediate) {
		pageLoaderProvider.get()
	}

	private fun getDefaultFileUri(proposedName: String?): DocumentFile? {
		if (settings.isPagesSavingAskEnabled) {
			return null
		}
		val dir = settings.getPagesSaveDir(context) ?: return null
		if (proposedName == null) {
			return dir
		} else {
			val mime = MimeTypes.getMimeTypeFromExtension(proposedName)?.toString() ?: return null
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

	private suspend fun getImageExtension(file: File): String? = runInterruptible(Dispatchers.IO) {
		MimeTypes.getExtension(BitmapDecoderCompat.probeMimeType(file))
	}

	data class Task(
		val manga: Manga,
		val chapterId: Long,
		val pageNumber: Int,
		val page: MangaPage,
	) {

		fun getFileBaseName() = buildString {
			append(manga.title.toFileNameSafe().take(MAX_BASENAME_LENGTH))
			manga.findChapterById(chapterId)?.let { chapter ->
				append('-')
				append(chapter.number)
			}
			append('-')
			append(pageNumber)
			append('_')
			append(SimpleDateFormat("yyyy-MM-dd_HHmm").format(Date()))
		}
	}

	@AssistedFactory
	interface Factory {

		fun create(activityResultCaller: ActivityResultCaller): PageSaveHelper
	}

	private companion object {

		private const val MAX_BASENAME_LENGTH = 12
		private const val EXTENSION_FALLBACK = "png"
		private const val TEMP_DIR = "pages"
	}
}
