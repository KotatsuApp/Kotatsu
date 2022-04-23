package org.koitharu.kotatsu.local.data

import android.net.Uri
import android.webkit.MimeTypeMap
import coil.bitmap.BitmapPool
import coil.decode.DataSource
import coil.decode.Options
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.size.Size
import java.util.zip.ZipFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import okio.buffer
import okio.source

class CbzFetcher : Fetcher<Uri> {

	override suspend fun fetch(
		pool: BitmapPool,
		data: Uri,
		size: Size,
		options: Options,
	): FetchResult = runInterruptible(Dispatchers.IO) {
		val zip = ZipFile(data.schemeSpecificPart)
		val entry = zip.getEntry(data.fragment)
		val ext = MimeTypeMap.getFileExtensionFromUrl(entry.name)
		SourceResult(
			source = ExtraCloseableBufferedSource(
				zip.getInputStream(entry).source().buffer(),
				zip,
			),
			mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext),
			dataSource = DataSource.DISK
		)
	}

	override fun key(data: Uri) = data.toString()

	override fun handles(data: Uri) = data.scheme == "cbz"
}