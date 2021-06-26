package org.koitharu.kotatsu.core.os

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ShortcutManager
import android.media.ThumbnailUtils
import android.os.Build
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import coil.ImageLoader
import coil.request.ImageRequest
import coil.size.PixelSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.domain.MangaDataRepository
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.history.domain.HistoryRepository
import org.koitharu.kotatsu.reader.ui.ReaderActivity
import org.koitharu.kotatsu.utils.ext.requireBitmap

class ShortcutsRepository(
	private val context: Context,
	private val coil: ImageLoader,
	private val historyRepository: HistoryRepository,
	private val mangaRepository: MangaDataRepository
) {

	private val iconSize by lazy {
		getIconSize(context)
	}

	suspend fun updateShortcuts() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return
		val manager = context.getSystemService(Context.SHORTCUT_SERVICE) as ShortcutManager
		val shortcuts = historyRepository.getList(0, manager.maxShortcutCountPerActivity)
			.filter { x -> x.title.isNotEmpty() }
			.map { buildShortcutInfo(it).build().toShortcutInfo() }
		manager.dynamicShortcuts = shortcuts
	}

	suspend fun requestPinShortcut(manga: Manga): Boolean {
		return ShortcutManagerCompat.requestPinShortcut(
			context,
			buildShortcutInfo(manga).build(),
			null
		)
	}

	private suspend fun buildShortcutInfo(manga: Manga): ShortcutInfoCompat.Builder {
		val icon = runCatching {
			withContext(Dispatchers.IO) {
				val bmp = coil.execute(
					ImageRequest.Builder(context)
						.data(manga.coverUrl)
						.size(iconSize)
						.build()
				).requireBitmap()
				ThumbnailUtils.extractThumbnail(bmp, iconSize.width, iconSize.height, 0)
			}
		}.fold(
			onSuccess = { IconCompat.createWithAdaptiveBitmap(it) },
			onFailure = { IconCompat.createWithResource(context, R.drawable.ic_shortcut_default) }
		)
		mangaRepository.storeManga(manga)
		return ShortcutInfoCompat.Builder(context, manga.id.toString())
			.setShortLabel(manga.title)
			.setLongLabel(manga.title)
			.setIcon(icon)
			.setIntent(
				ReaderActivity.newIntent(context, manga.id, null)
					.setAction(ReaderActivity.ACTION_MANGA_READ)
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
}