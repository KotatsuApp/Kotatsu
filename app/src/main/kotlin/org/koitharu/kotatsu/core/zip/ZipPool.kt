package org.koitharu.kotatsu.core.zip

import android.net.Uri
import androidx.annotation.WorkerThread
import androidx.collection.LruCache
import okhttp3.internal.closeQuietly
import okio.Source
import okio.source
import java.io.File
import java.util.zip.ZipFile

class ZipPool(maxSize: Int) : LruCache<String, ZipFile>(maxSize) {

	override fun entryRemoved(evicted: Boolean, key: String, oldValue: ZipFile, newValue: ZipFile?) {
		super.entryRemoved(evicted, key, oldValue, newValue)
		oldValue.closeQuietly()
	}

	override fun create(key: String): ZipFile {
		return ZipFile(File(key), ZipFile.OPEN_READ)
	}

	@Synchronized
	@WorkerThread
	operator fun get(uri: Uri): Source {
		val zip = requireNotNull(get(uri.schemeSpecificPart)) {
			"Cannot obtain zip by \"$uri\""
		}
		val entry = zip.getEntry(uri.fragment)
		return zip.getInputStream(entry).source()
	}
}
