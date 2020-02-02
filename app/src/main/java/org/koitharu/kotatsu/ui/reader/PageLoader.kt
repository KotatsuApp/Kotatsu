package org.koitharu.kotatsu.ui.reader

import android.content.Context
import android.util.LongSparseArray
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.KoinComponent
import org.koin.core.inject
import org.koitharu.kotatsu.core.model.MangaPage
import org.koitharu.kotatsu.utils.ext.await
import org.koitharu.kotatsu.utils.ext.longHashCode
import java.io.Closeable
import java.io.File
import kotlin.coroutines.CoroutineContext

class PageLoader(context: Context) : KoinComponent, CoroutineScope, DisposableHandle {

	private val job = SupervisorJob()
	private val tasks = HashMap<String, Job>()
	private val okHttp by inject<OkHttpClient>()
	private val cacheDir = File(context.externalCacheDir ?: context.cacheDir, "pages")

	init {
		if (!cacheDir.exists()) {
			cacheDir.mkdir()
		}
	}

	override val coroutineContext: CoroutineContext
		get() = Dispatchers.Main + job

	fun load(url: String, callback: (File) -> Unit) = launch {
		val result = withContext(Dispatchers.IO) {
			loadFile(url, false)
		}
		callback(result)
	}

	private suspend fun loadFile(url: String, force: Boolean): File {
		val file = File(cacheDir, url.longHashCode().toString())
		if (!force && file.exists()) {
			return file
		}
		val request = Request.Builder()
			.url(url)
			.get()
			.build()
		okHttp.newCall(request).await().use { response ->
			file.outputStream().use { out ->
				response.body!!.byteStream().copyTo(out)
			}
			return file
		}
	}

	override fun dispose() {
		coroutineContext.cancel()
	}
}