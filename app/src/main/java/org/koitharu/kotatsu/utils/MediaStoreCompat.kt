package org.koitharu.kotatsu.utils

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.database.getStringOrNull
import org.koitharu.kotatsu.BuildConfig
import java.io.OutputStream

object MediaStoreCompat {

	@JvmStatic
	fun insertImage(
		resolver: ContentResolver,
		fileName: String,
		block: (OutputStream) -> Unit
	): Uri? {
		val name = fileName.substringBeforeLast('.')
		val cv = ContentValues(7)
		cv.put(MediaStore.Images.Media.DISPLAY_NAME, name)
		cv.put(MediaStore.Images.Media.TITLE, name)
		cv.put(
			MediaStore.Images.Media.MIME_TYPE,
			MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileName.substringAfterLast('.'))
		)
		cv.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis())
		cv.put(MediaStore.Images.Media.DATE_MODIFIED, System.currentTimeMillis())
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			cv.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
			cv.put(MediaStore.Images.Media.IS_PENDING, 1)
		}
		var uri: Uri? = null
		try {
			uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv)
			resolver.openOutputStream(uri!!)?.use(block)
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
				cv.clear()
				cv.put(MediaStore.Images.Media.IS_PENDING, 0)
				resolver.update(uri, cv, null, null)
			}
		} catch (e: Exception) {
			if (BuildConfig.DEBUG) {
				e.printStackTrace()
			}
			uri?.let {
				resolver.delete(it, null, null)
			}
			uri = null
		}
		return uri
	}

	@JvmStatic
	fun getName(context: Context, uri: Uri): String? = (if (uri.scheme == "content") {
		context.contentResolver.query(uri, null, null, null, null)?.use {
			if (it.moveToFirst()) {
				it.getStringOrNull(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
			} else {
				null
			}
		}
	} else {
		null
	}) ?: uri.path?.substringAfterLast('/')
}