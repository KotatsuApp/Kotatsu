package org.koitharu.kotatsu.core.local

import android.content.Context
import org.koitharu.kotatsu.utils.ext.longHashCode
import org.koitharu.kotatsu.utils.ext.sub
import org.koitharu.kotatsu.utils.ext.takeIfReadable
import java.io.File
import java.io.OutputStream

class PagesCache(context: Context) {

	private val cacheDir = File(context.externalCacheDir ?: context.cacheDir, "pages")

	init {
		if (!cacheDir.exists()) {
			cacheDir.mkdir()
		}
	}

	operator fun get(url: String) = cacheDir.sub(url.longHashCode().toString()).takeIfReadable()

	fun put(url: String, writer: (OutputStream) -> Unit): File {
		val file = cacheDir.sub(url.longHashCode().toString())
		file.outputStream().use(writer)
		return file
	}
}