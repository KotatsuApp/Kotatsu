package org.koitharu.kotatsu.core.os

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ShortcutManager
import android.media.ThumbnailUtils
import android.os.Build
import android.util.Size
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.room.InvalidationTracker
import coil.ImageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.domain.MangaDataRepository
import org.koitharu.kotatsu.core.db.TABLE_HISTORY
import org.koitharu.kotatsu.history.domain.HistoryRepository
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.reader.ui.ReaderActivity
import org.koitharu.kotatsu.utils.ext.printStackTraceDebug
import org.koitharu.kotatsu.utils.ext.processLifecycleScope
import org.koitharu.kotatsu.utils.ext.requireBitmap
import org.koitharu.kotatsu.utils.ext.runCatchingCancellable

class ShortcutsUpdater(
	private val context: Context,
	private val coil: ImageLoader,
	private val historyRepository: HistoryRepository,
	private val mangaRepository: MangaDataRepository,
) : InvalidationTracker.Observer(TABLE_HISTORY) {

	private val iconSize by lazy { getIconSize(context) }
	private var shortcutsUpdateJob: Job? = null

	override fun onInvalidated(tables: MutableSet<String>) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
			val prevJob = shortcutsUpdateJob
			shortcutsUpdateJob = processLifecycleScope.launch(Dispatchers.Default) {
				prevJob?.join()
				updateShortcutsImpl()
			}
		}
	}

	suspend fun requestPinShortcut(manga: Manga): Boolean {
		return ShortcutManagerCompat.requestPinShortcut(
			context,
			buildShortcutInfo(manga).build(),
			null,
		)
	}

	@VisibleForTesting
	suspend fun await(): Boolean {
		return shortcutsUpdateJob?.join() != null
	}

	@RequiresApi(Build.VERSION_CODES.N_MR1)
	private suspend fun updateShortcutsImpl() = runCatchingCancellable {
		val manager = context.getSystemService(Context.SHORTCUT_SERVICE) as ShortcutManager
		val shortcuts = historyRepository.getList(0, manager.maxShortcutCountPerActivity)
			.filter { x -> x.title.isNotEmpty() }
			.map { buildShortcutInfo(it).build().toShortcutInfo() }
		manager.dynamicShortcuts = shortcuts
	}.onFailure {
		it.printStackTraceDebug()
	}

	private suspend fun buildShortcutInfo(manga: Manga): ShortcutInfoCompat.Builder {
		val icon = runCatchingCancellable {
			val bmp = coil.execute(
				ImageRequest.Builder(context)
					.data(manga.coverUrl)
					.size(iconSize.width, iconSize.height)
					.build(),
			).requireBitmap()
			ThumbnailUtils.extractThumbnail(bmp, iconSize.width, iconSize.height, 0)
		}.fold(
			onSuccess = { IconCompat.createWithAdaptiveBitmap(it) },
			onFailure = { IconCompat.createWithResource(context, R.drawable.ic_shortcut_default) },
		)
		mangaRepository.storeManga(manga)
		return ShortcutInfoCompat.Builder(context, manga.id.toString())
			.setShortLabel(manga.title)
			.setLongLabel(manga.title)
			.setIcon(icon)
			.setIntent(
				ReaderActivity.newIntent(context, manga.id)
					.setAction(ReaderActivity.ACTION_MANGA_READ),
			)
	}

	private fun getIconSize(context: Context): Size {
		return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
			(context.getSystemService(Context.SHORTCUT_SERVICE) as ShortcutManager).let {
				Size(it.iconMaxWidth, it.iconMaxHeight)
			}
		} else {
			(context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).launcherLargeIconSize.let {
				Size(it, it)
			}
		}
	}
}