package org.koitharu.kotatsu.reader.domain

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.collection.LongSparseArray
import androidx.collection.set
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.component.KoinComponent
import org.koitharu.kotatsu.core.model.MangaPage
import org.koitharu.kotatsu.core.network.CommonHeaders
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.local.data.PagesCache
import org.koitharu.kotatsu.utils.CacheUtils
import org.koitharu.kotatsu.utils.ext.await
import org.koitharu.kotatsu.utils.ext.mangaRepositoryOf
import java.io.File
import java.util.zip.ZipFile

class PageLoader(
	scope: CoroutineScope,
	private val okHttp: OkHttpClient,
	private val cache: PagesCache
) : CoroutineScope by scope, KoinComponent {

	private var repository: MangaRepository? = null
	private val tasks = LongSparseArray<Deferred<File>>()
	private val convertLock = Mutex()

	suspend fun loadPage(page: MangaPage, force: Boolean): File {
		if (!force) {
			cache[page.url]?.let {
				return it
			}
		}
		var task = tasks[page.id]
		if (force) {
			task?.cancel()
		} else if (task?.isCancelled == false) {
			return task.await()
		}
		task = loadAsync(page)
		tasks[page.id] = task
		return task.await()
	}

	private fun loadAsync(page: MangaPage): Deferred<File> {
		var repo = repository
		if (repo?.source != page.source) {
			repo = mangaRepositoryOf(page.source)
			repository = repo
		}
		return async(Dispatchers.IO) {
			val pageUrl = repo.getPageUrl(page)
			check(pageUrl.isNotBlank()) { "Cannot obtain full image url" }
			val uri = Uri.parse(pageUrl)
			if (uri.scheme == "cbz") {
				val zip = ZipFile(uri.schemeSpecificPart)
				val entry = zip.getEntry(uri.fragment)
				zip.getInputStream(entry).use {
					cache.put(pageUrl, it)
				}
			} else {
				val request = Request.Builder()
					.url(pageUrl)
					.get()
					.header(CommonHeaders.REFERER, page.referer)
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
						cache.put(pageUrl, it)
					}
				}
			}
		}
	}

	suspend fun convertInPlace(file: File) {
		convertLock.withLock(Lock) {
			withContext(Dispatchers.Default) {
				val image = BitmapFactory.decodeFile(file.absolutePath)
				try {
					file.outputStream().use { out ->
						image.compress(Bitmap.CompressFormat.PNG, 100, out)
					}
				} finally {
					image.recycle()
				}
			}
		}
	}

	private companion object Lock
}