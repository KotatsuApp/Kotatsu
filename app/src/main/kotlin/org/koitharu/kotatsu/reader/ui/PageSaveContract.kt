package org.koitharu.kotatsu.reader.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.webkit.MimeTypeMap
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import org.koitharu.kotatsu.core.util.ext.toUriOrNull
import java.io.File

class PageSaveContract : ActivityResultContracts.CreateDocument("image/*") {

	override fun createIntent(context: Context, input: String): Intent {
		val intent = super.createIntent(context, input.substringAfterLast(File.separatorChar))
		intent.type = MimeTypeMap.getSingleton()
			.getMimeTypeFromExtension(input.substringAfterLast('.')) ?: "image/*"
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			val defaultUri = input.toUriOrNull()?.run {
				path?.let { p ->
					buildUpon().path(p.substringBeforeLast('/')).build()
				}
			}
			intent.putExtra(
				DocumentsContract.EXTRA_INITIAL_URI,
				defaultUri ?: Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toUri(),
			)
		}
		return intent
	}
}
