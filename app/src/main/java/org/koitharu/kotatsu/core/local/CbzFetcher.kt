package org.koitharu.kotatsu.core.local

import android.net.Uri
import android.webkit.MimeTypeMap
import coil.bitmappool.BitmapPool
import coil.decode.DataSource
import coil.decode.Options
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.size.Size
import okio.buffer
import okio.source
import java.util.zip.ZipFile

class CbzFetcher : Fetcher<Uri> {

	@Suppress("BlockingMethodInNonBlockingContext")
	override suspend fun fetch(
		pool: BitmapPool,
		data: Uri,
		size: Size,
		options: Options
	): FetchResult {
		val zip = ZipFile(data.schemeSpecificPart)
		val entry = zip.getEntry(data.fragment)
		val ext = MimeTypeMap.getFileExtensionFromUrl(entry.name)
		return SourceResult(
			source = zip.getInputStream(entry).source().buffer(),
			mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext),
			dataSource = DataSource.DISK
		)
	}

	override fun key(data: Uri): String? = data.toString()

	override fun handles(data: Uri) = data.scheme == "cbz"
}