package org.koitharu.kotatsu.utils

import android.app.ActivityManager
import android.content.Context
import androidx.core.content.getSystemService
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import coil.Coil
import coil.api.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.ui.details.MangaDetailsActivity
import org.koitharu.kotatsu.utils.ext.safe

object ShortcutUtils {

	suspend fun createShortcutInfo(context: Context, manga: Manga): ShortcutInfoCompat {
		val icon = safe {
			withContext(Dispatchers.IO) {
				Coil.loader().get(manga.coverUrl) {
					context.getSystemService<ActivityManager>()?.let {
						size(it.launcherLargeIconSize)
					}
				}.toBitmap()
			}
		}
		return ShortcutInfoCompat.Builder(context, manga.id.toString())
			.setShortLabel(manga.title)
			.setLongLabel(manga.title)
			.setIcon(icon?.let {
				IconCompat.createWithBitmap(it)
			} ?: IconCompat.createWithResource(context, R.drawable.ic_launcher_foreground))
			.setIntent(
				MangaDetailsActivity.newIntent(context, manga.copy(chapters = null))
					.setAction(MangaDetailsActivity.ACTION_MANGA_VIEW)
			)
			.build()
	}
}