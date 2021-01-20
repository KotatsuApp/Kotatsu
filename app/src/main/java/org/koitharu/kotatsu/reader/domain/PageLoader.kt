package org.koitharu.kotatsu.reader.domain

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.ArrayMap
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import org.koitharu.kotatsu.core.model.RequestDraft
import org.koitharu.kotatsu.core.network.CommonHeaders
import org.koitharu.kotatsu.local.data.PagesCache
import org.koitharu.kotatsu.utils.CacheUtils
import org.koitharu.kotatsu.utils.ext.await
import java.io.File
import java.util.zip.ZipFile

class PageLoader(
	scope: CoroutineScope,
	private val okHttp: OkHttpClient,
	private val cache: PagesCache
) : CoroutineScope by scope {

	private val tasks = ArrayMap<String, Deferred<File>>()
	private val convertLock = Mutex()

	suspend fun loadFile(requestDraft: RequestDraft, force: Boolean): File {
		if (!force) {
			cache[requestDraft.url]?.let {
				return it
			}
		}
		val task =
			tasks[requestDraft.url]?.takeUnless { it.isCancelled || (force && it.isCompleted) }
		return (task ?: loadAsync(requestDraft).also { tasks[requestDraft.url] = it }).await()
	}

	private fun loadAsync(requestDraft: RequestDraft) = async(Dispatchers.IO) {
		val uri = Uri.parse(requestDraft.url)
		if (uri.scheme == "cbz") {
			val zip = ZipFile(uri.schemeSpecificPart)
			val entry = zip.getEntry(uri.fragment)
			zip.getInputStream(entry).use {
				cache.put(requestDraft.url, it)
			}
		} else {
			val request = requestDraft.newBuilder()
				.header(CommonHeaders.ACCEPT, "image/webp,image/png;q=0.9,image/jpeg,*/*;q=0.8")
				.cacheControl(CacheUtils.CONTROL_DISABLED)
				.build()
			okHttp.newCall(request).await().use { response ->
				check(response.isSuccessful) {
					"Invalid response: ${response.code} ${response.message}"
				}
				val body = checkNotNull(response.body) {
					"Null response"
				}
				body.byteStream().use {
					cache.put(requestDraft.url, it)
				}
			}
		}
	}

	suspend fun convertInPlace(file: File) {
		convertLock.withLock(file) {
			withContext(Dispatchers.Default) {
				val image = BitmapFactory.decodeFile(file.absolutePath)
				try {
					file.outputStream().use { out ->
						image.compress(Bitmap.CompressFormat.WEBP, 100, out)
					}
				} finally {
					image.recycle()
				}
			}
		}
	}
}