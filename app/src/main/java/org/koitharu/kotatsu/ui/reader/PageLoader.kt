package org.koitharu.kotatsu.ui.reader

import android.net.Uri
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.KoinComponent
import org.koin.core.inject
import org.koitharu.kotatsu.core.local.PagesCache
import org.koitharu.kotatsu.utils.CacheUtils
import org.koitharu.kotatsu.utils.ext.await
import java.io.File
import java.util.zip.ZipFile
import kotlin.coroutines.CoroutineContext

class PageLoader : KoinComponent, CoroutineScope, DisposableHandle {

	private val job = SupervisorJob()
	private val tasks = HashMap<String, Deferred<File>>()
	private val okHttp by inject<OkHttpClient>()
	private val cache by inject<PagesCache>()

	override val coroutineContext: CoroutineContext
		get() = Dispatchers.Main + job

	@Suppress("BlockingMethodInNonBlockingContext")
	suspend fun loadFile(url: String, force: Boolean): File {
		if (!force) {
			cache[url]?.let {
				return it
			}
		}
		val task = tasks[url]?.takeUnless { it.isCancelled }
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
				.cacheControl(CacheUtils.CONTROL_DISABLED)
				.build()
			okHttp.newCall(request).await().use { response ->
				val body = response.body
				checkNotNull(body) {
					"Null response"
				}
				cache.put(url) { out ->
					body.byteStream().copyTo(out)
				}
			}
		}
	}

	override fun dispose() {
		coroutineContext.cancel()
		tasks.clear()
	}
}