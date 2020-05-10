package org.koitharu.kotatsu.utils

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ShortcutManager
import android.media.ThumbnailUtils
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import coil.Coil
import coil.request.GetRequestBuilder
import coil.size.PixelSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.domain.MangaDataRepository
import org.koitharu.kotatsu.ui.details.MangaDetailsActivity
import org.koitharu.kotatsu.utils.ext.requireBitmap
import org.koitharu.kotatsu.utils.ext.safe

class MangaShortcut(private val manga: Manga) {

	private val shortcutId = manga.id.toString()

	@RequiresApi(Build.VERSION_CODES.N_MR1)
	suspend fun addAppShortcut(context: Context) {
		val manager = context.getSystemService(Context.SHORTCUT_SERVICE) as ShortcutManager
		val limit = manager.maxShortcutCountPerActivity
		val builder = buildShortcutInfo(context, manga)
		val shortcuts = manager.dynamicShortcuts
		for (shortcut in shortcuts) {
			if (shortcut.id == shortcutId) {
				builder.setRank(shortcut.rank + 1)
				manager.updateShortcuts(listOf(builder.build().toShortcutInfo()))
				return
			}
		}
		builder.setRank(1)
		if (shortcuts.isNotEmpty() && shortcuts.size >= limit) {
			manager.removeDynamicShortcuts(listOf(shortcuts.minBy { it.rank }!!.id))
		}
		manager.addDynamicShortcuts(listOf(builder.build().toShortcutInfo()))
	}

	suspend fun requestPinShortcut(context: Context): Boolean {
		return ShortcutManagerCompat.requestPinShortcut(
			context,
			buildShortcutInfo(context, manga).build(),
			null
		)
	}

	@RequiresApi(Build.VERSION_CODES.N_MR1)
	fun removeAppShortcut(context: Context) {
		val manager = context.getSystemService(Context.SHORTCUT_SERVICE) as ShortcutManager
		manager.removeDynamicShortcuts(listOf(shortcutId))
	}

	private suspend fun buildShortcutInfo(
		context: Context,
		manga: Manga
	): ShortcutInfoCompat.Builder {
		val icon = safe {
			val size = getIconSize(context)
			withContext(Dispatchers.IO) {
				val bmp = Coil.execute(GetRequestBuilder(context)
					.data(manga.coverUrl)
					.build()).requireBitmap()
				ThumbnailUtils.extractThumbnail(bmp, size.width, size.height, 0)
			}
		}
		MangaDataRepository().storeManga(manga)
		return ShortcutInfoCompat.Builder(context, manga.id.toString())
			.setShortLabel(manga.title)
			.setLongLabel(manga.title)
			.setIcon(icon?.let {
				IconCompat.createWithAdaptiveBitmap(it)
			} ?: IconCompat.createWithResource(context, R.drawable.ic_launcher_foreground))
			.setIntent(
				MangaDetailsActivity.newIntent(context, manga.id)
					.setAction(MangaDetailsActivity.ACTION_MANGA_VIEW)
			)
	}

	private fun getIconSize(context: Context): PixelSize {
		return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
			(context.getSystemService(Context.SHORTCUT_SERVICE) as ShortcutManager).let {
				PixelSize(it.iconMaxWidth, it.iconMaxHeight)
			}
		} else {
			(context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).launcherLargeIconSize.let {
				PixelSize(it, it)
			}
		}
	}

	companion object {

		@RequiresApi(Build.VERSION_CODES.N_MR1)
		fun clearAppShortcuts(context: Context) {
			val manager = context.getSystemService(Context.SHORTCUT_SERVICE) as ShortcutManager
			manager.removeAllDynamicShortcuts()
		}
	}
}