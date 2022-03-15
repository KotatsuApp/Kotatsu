package org.koitharu.kotatsu.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.parsers.model.Manga
import java.io.File

class ShareHelper(private val context: Context) {

	fun shareMangaLink(manga: Manga) {
		val intent = Intent(Intent.ACTION_SEND)
		intent.type = "text/plain"
		intent.putExtra(Intent.EXTRA_TEXT, buildString {
			append(manga.title)
			append("\n \n")
			append(manga.publicUrl)
		})
		val shareIntent =
			Intent.createChooser(intent, context.getString(R.string.share_s, manga.title))
		context.startActivity(shareIntent)
	}

	fun shareCbz(file: File) {
		val uri = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.files", file)
		val intent = Intent(Intent.ACTION_SEND)
		intent.setDataAndType(uri, context.contentResolver.getType(uri))
		intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
		val shareIntent =
			Intent.createChooser(intent, context.getString(R.string.share_s, file.name))
		context.startActivity(shareIntent)
	}

	fun shareBackup(file: File) {
		val uri = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.files", file)
		val intent = Intent(Intent.ACTION_SEND)
		intent.setDataAndType(uri, context.contentResolver.getType(uri))
		intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
		val shareIntent =
			Intent.createChooser(intent, context.getString(R.string.share_s, file.name))
		context.startActivity(shareIntent)
	}

	fun shareImage(uri: Uri) {
		val intent = Intent(Intent.ACTION_SEND)
		intent.setDataAndType(uri, context.contentResolver.getType(uri) ?: "image/*")
		intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
		val shareIntent = Intent.createChooser(intent, context.getString(R.string.share_image))
		context.startActivity(shareIntent)
	}

	fun shareText(text: String) {
		val intent = Intent(Intent.ACTION_SEND)
		intent.type = "text/plain"
		intent.putExtra(Intent.EXTRA_TEXT, text)
		val shareIntent = Intent.createChooser(intent, context.getString(R.string.share))
		context.startActivity(shareIntent)
	}
}