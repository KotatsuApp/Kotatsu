package org.koitharu.kotatsu.local.data

import android.net.Uri
import android.webkit.MimeTypeMap
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import okhttp3.internal.closeQuietly
import okio.buffer
import okio.source
import org.koitharu.kotatsu.local.data.util.withExtraCloseable
import java.util.zip.ZipFile

class CbzFetcher(
	private val uri: Uri,
	private val options: Options
) : Fetcher {

	override suspend fun fetch() = runInterruptible(Dispatchers.IO) {
		val zip = ZipFile(uri.schemeSpecificPart)
		try {
			val entry = zip.getEntry(uri.fragment)
			val ext = MimeTypeMap.getFileExtensionFromUrl(entry.name)
			val bufferedSource = zip.getInputStream(entry).source().withExtraCloseable(zip).buffer()
			SourceResult(
				source = ImageSource(
					source = bufferedSource,
					context = options.context,
					metadata = CbzMetadata(uri),
				),
				mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext),
				dataSource = DataSource.DISK,
			)
		} catch (e: Throwable) {
			zip.closeQuietly()
			throw e
		}
	}

	class Factory : Fetcher.Factory<Uri> {

		override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
			return if (data.scheme == "cbz") {
				CbzFetcher(data, options)
			} else {
				null
			}
		}
	}

	class CbzMetadata(val uri: Uri) : ImageSource.Metadata()
}
