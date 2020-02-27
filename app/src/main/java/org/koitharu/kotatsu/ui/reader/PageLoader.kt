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
	private val tasks = HashMap<String, Job>()
	private val okHttp by inject<OkHttpClient>()
	private val cache by inject<PagesCache>()

	override val coroutineContext: CoroutineContext
		get() = Dispatchers.Main + job

	suspend fun loadFile(url: String, force: Boolean): File {
		if (!force) {
			cache[url]?.let {
				return it
			}
		}
		val uri = Uri.parse(url)
		return if (uri.scheme == "cbz") {
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
				val body = response.body!!
				val type = body.contentType()
				check(type?.type == "image") {
					"Unexpected content type ${type?.type}/${type?.subtype}"
				}
				cache.put(url) { out ->
					response.body!!.byteStream().copyTo(out)
				}
			}
		}
	}

	override fun dispose() {
		coroutineContext.cancel()
	}
}