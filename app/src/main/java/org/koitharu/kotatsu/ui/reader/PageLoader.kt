package org.koitharu.kotatsu.ui.reader

import android.content.Context
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.KoinComponent
import org.koin.core.inject
import org.koitharu.kotatsu.core.local.PagesCache
import org.koitharu.kotatsu.utils.ext.await
import java.io.File
import kotlin.coroutines.CoroutineContext

class PageLoader(context: Context) : KoinComponent, CoroutineScope, DisposableHandle {

	private val job = SupervisorJob()
	private val tasks = HashMap<String, Job>()
	private val okHttp by inject<OkHttpClient>()
	private val cache by inject<PagesCache>()

	override val coroutineContext: CoroutineContext
		get() = Dispatchers.Main + job

	fun load(url: String, callback: (File) -> Unit) = launch {
		val result = withContext(Dispatchers.IO) {
			loadFile(url, false)
		}
		callback(result)
	}

	private suspend fun loadFile(url: String, force: Boolean): File {
		if (!force) {
			cache[url]?.let {

				return it
			}
		}
		val request = Request.Builder()
			.url(url)
			.get()
			.build()
		return okHttp.newCall(request).await().use { response ->
			cache.put(url) { out ->
				response.body!!.byteStream().copyTo(out)
			}
		}
	}

	override fun dispose() {
		coroutineContext.cancel()
	}
}