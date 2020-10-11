package org.koitharu.kotatsu.ui.reader

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.ArrayMap
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koitharu.kotatsu.core.local.PagesCache
import org.koitharu.kotatsu.utils.CacheUtils
import org.koitharu.kotatsu.utils.ext.await
import java.io.File
import java.util.zip.ZipFile
import kotlin.coroutines.CoroutineContext

class PageLoader : KoinComponent, CoroutineScope, DisposableHandle {

	private val job = SupervisorJob()
	private val tasks = ArrayMap<String, Deferred<File>>()
	private val okHttp by inject<OkHttpClient>()
	private val cache by inject<PagesCache>()
	private val convertLock = Mutex()

	override val coroutineContext: CoroutineContext
		get() = Dispatchers.Main.immediate + job

	@Suppress("BlockingMethodInNonBlockingContext")
	suspend fun loadFile(url: String, force: Boolean): File {
		if (!force) {
			cache[url]?.let {
				return it
			}
		}
		val task = tasks[url]?.takeUnless { it.isCancelled || (force && it.isCompleted) }
		return (task ?: loadAsync(url).also { tasks[url] = it }).await()
	}

	private fun loadAsync(url: String) = async(Dispatchers.IO) {
		val uri = Uri.parse(url)
		if (uri.scheme == "cbz") {
			val zip = ZipFile(uri.schemeSpecificPart)
			val entry = zip.getEntry(uri.fragment)
			zip.getInputStream(entry).use {
				cache.put(url) { out ->
					it.copyTo(out)
				}
			}
		} else {
			val request = Request.Builder()
				.url(url)
				.get()
				.header("Accept", "image/webp,image/png;q=0.9,image/jpeg,*/*;q=0.8")
				.cacheControl(CacheUtils.CONTROL_DISABLED)
				.build()
			okHttp.newCall(request).await().use { response ->
				val body = response.body
				check(response.isSuccessful) {
					"Invalid response: ${response.code} ${response.message}"
				}
				checkNotNull(body) {
					"Null response"
				}
				cache.put(url) { out ->
					body.byteStream().copyTo(out)
				}
			}
		}
	}

	suspend fun convertInPlace(file: File) {
		convertLock.withLock(file) {
			withContext(Dispatchers.IO) {
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

	override fun dispose() {
		coroutineContext.cancel()
		tasks.clear()
	}
}