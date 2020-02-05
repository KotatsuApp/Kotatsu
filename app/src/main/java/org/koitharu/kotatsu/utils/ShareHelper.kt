package org.koitharu.kotatsu.utils

import android.content.Context
import android.content.Intent
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.Manga

object ShareHelper {

	@JvmStatic
	fun shareMangaLink(context: Context, manga: Manga) {
		val intent = Intent(Intent.ACTION_SEND)
		intent.type = "text/plain"
		intent.putExtra(Intent.EXTRA_TEXT, buildString {
			append(manga.title)
			append("\n \n")
			append(manga.url)
		})
		val shareIntent = Intent.createChooser(intent, context.getString(R.string.share_s, manga.title))
		context.startActivity(shareIntent)
	}
}