package org.koitharu.kotatsu.core.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.appUrl
import org.koitharu.kotatsu.parsers.model.Manga
import java.io.File

private const val TYPE_TEXT = "text/plain"
private const val TYPE_IMAGE = "image/*"
private const val TYPE_CBZ = "application/x-cbz"

@Deprecated("")
class ShareHelper(private val context: Context) {

	fun shareMangaLink(manga: Manga) {
		val text = buildString {
			append(manga.title)
			append("\n \n")
			append(manga.publicUrl)
			append("\n \n")
			append(manga.appUrl)
		}
		ShareCompat.IntentBuilder(context)
			.setText(text)
			.setType(TYPE_TEXT)
			.setChooserTitle(context.getString(R.string.share_s, manga.title))
			.startChooser()
	}

	fun shareMangaLinks(manga: Collection<Manga>) {
		if (manga.isEmpty()) {
			return
		}
		if (manga.size == 1) {
			shareMangaLink(manga.first())
			return
		}
		val text = manga.joinToString("\n \n") {
			"${it.title} - ${it.publicUrl}"
		}
		ShareCompat.IntentBuilder(context)
			.setText(text)
			.setType(TYPE_TEXT)
			.setChooserTitle(R.string.share)
			.startChooser()
	}

	fun shareCbz(files: Collection<File>) {
		if (files.isEmpty()) {
			return
		}
		val intentBuilder = ShareCompat.IntentBuilder(context)
			.setType(TYPE_CBZ)
		for (file in files) {
			val uri = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.files", file)
			intentBuilder.addStream(uri)
		}
		files.singleOrNull()?.let {
			intentBuilder.setChooserTitle(context.getString(R.string.share_s, it.name))
		} ?: run {
			intentBuilder.setChooserTitle(R.string.share)
		}
		intentBuilder.startChooser()
	}

	fun shareImage(uri: Uri) {
		ShareCompat.IntentBuilder(context)
			.setStream(uri)
			.setType(context.contentResolver.getType(uri) ?: TYPE_IMAGE)
			.setChooserTitle(R.string.share_image)
			.startChooser()
	}

	fun getShareTextIntent(text: String): Intent = ShareCompat.IntentBuilder(context)
		.setText(text)
		.setType(TYPE_TEXT)
		.setChooserTitle(R.string.share)
		.createChooserIntent()
}
