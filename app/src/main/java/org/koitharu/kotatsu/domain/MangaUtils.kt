package org.koitharu.kotatsu.domain

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Size
import androidx.annotation.WorkerThread
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.core.model.MangaPage
import org.koitharu.kotatsu.utils.ext.await
import org.koitharu.kotatsu.utils.ext.medianOrNull
import java.io.InputStream
import java.util.zip.ZipFile

object MangaUtils : KoinComponent {

	/**
	 * Automatic determine type of manga by page size
	 * @return ReaderMode.WEBTOON if page is wide
	 */
	@WorkerThread
	@Suppress("BlockingMethodInNonBlockingContext")
	suspend fun determineMangaIsWebtoon(pages: List<MangaPage>): Boolean? {
		try {
			val page = pages.medianOrNull() ?: return null
			val url = page.source.repository.getPageFullUrl(page)
			val uri = Uri.parse(url)
			val size = if (uri.scheme == "cbz") {
				val zip = ZipFile(uri.schemeSpecificPart)
				val entry = zip.getEntry(uri.fragment)
				zip.getInputStream(entry).use {
					getBitmapSize(it)
				}
			} else {
				val client = get<OkHttpClient>()
				val request = Request.Builder()
					.url(url)
					.get()
					.build()
				client.newCall(request).await().use {
					getBitmapSize(it.body?.byteStream())
				}
			}
			return size.width * 2 < size.height
		} catch (e: Exception) {
			if (BuildConfig.DEBUG) {
				e.printStackTrace()
			}
			return null
		}
	}

	@JvmStatic
	private fun getBitmapSize(input: InputStream?): Size {
		val options = BitmapFactory.Options().apply {
			inJustDecodeBounds = true
		}
		BitmapFactory.decodeStream(input, null, options)
		val imageHeight: Int = options.outHeight
		val imageWidth: Int = options.outWidth
		check(imageHeight > 0 && imageWidth > 0)
		return Size(imageWidth, imageHeight)
	}
}