package org.koitharu.kotatsu.core.os

import android.app.ActivityManager
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ShortcutManager
import android.os.Build
import android.util.Size
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.room.InvalidationTracker
import coil.ImageLoader
import coil.request.ImageRequest
import coil.size.Scale
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.db.TABLE_HISTORY
import org.koitharu.kotatsu.core.parser.MangaDataRepository
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.ui.image.ThumbnailTransformation
import org.koitharu.kotatsu.core.util.ext.getDrawableOrThrow
import org.koitharu.kotatsu.core.util.ext.printStackTraceDebug
import org.koitharu.kotatsu.core.util.ext.processLifecycleScope
import org.koitharu.kotatsu.history.data.HistoryRepository
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
import org.koitharu.kotatsu.reader.ui.ReaderActivity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppShortcutManager @Inject constructor(
	@ApplicationContext private val context: Context,
	private val coil: ImageLoader,
	private val historyRepository: HistoryRepository,
	private val mangaRepository: MangaDataRepository,
	private val settings: AppSettings,
) : InvalidationTracker.Observer(TABLE_HISTORY), SharedPreferences.OnSharedPreferenceChangeListener {

	private val iconSize by lazy { getIconSize(context) }
	private var shortcutsUpdateJob: Job? = null

	init {
		settings.subscribe(this)
	}

	override fun onInvalidated(tables: Set<String>) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1 || !settings.isDynamicShortcutsEnabled) {
			return
		}
		val prevJob = shortcutsUpdateJob
		shortcutsUpdateJob = processLifecycleScope.launch(Dispatchers.Default) {
			prevJob?.join()
			updateShortcutsImpl()
		}
	}

	override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1 && key == AppSettings.KEY_SHORTCUTS) {
			if (settings.isDynamicShortcutsEnabled) {
				onInvalidated(emptySet())
			} else {
				clearShortcuts()
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

	fun isDynamicShortcutsAvailable(): Boolean {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
			return false
		}
		val manager = context.getSystemService(Context.SHORTCUT_SERVICE) as ShortcutManager
		return manager.maxShortcutCountPerActivity > 0
	}

	fun notifyMangaOpened(mangaId: Long) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
			return
		}
		val manager = context.getSystemService(Context.SHORTCUT_SERVICE) as ShortcutManager
		manager.reportShortcutUsed(mangaId.toString())
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

	@RequiresApi(Build.VERSION_CODES.N_MR1)
	private fun clearShortcuts() {
		val manager = context.getSystemService(Context.SHORTCUT_SERVICE) as ShortcutManager
		try {
			manager.removeAllDynamicShortcuts()
		} catch (_: IllegalStateException) {
		}
	}

	private suspend fun buildShortcutInfo(manga: Manga): ShortcutInfoCompat.Builder {
		val icon = runCatchingCancellable {
			coil.execute(
				ImageRequest.Builder(context)
					.data(manga.coverUrl)
					.size(iconSize.width, iconSize.height)
					.tag(manga.source)
					.scale(Scale.FILL)
					.transformations(ThumbnailTransformation())
					.build(),
			).getDrawableOrThrow().toBitmap()
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
				ReaderActivity.IntentBuilder(context)
					.mangaId(manga.id)
					.build(),
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
