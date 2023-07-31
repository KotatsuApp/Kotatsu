package org.koitharu.kotatsu.core.os

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ShortcutManager
import android.os.Build
import android.util.Size
import androidx.annotation.VisibleForTesting
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.util.component1
import androidx.core.util.component2
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

	private val iconWidthAndHeight by lazy {
		Size(ShortcutManagerCompat.getIconMaxWidth(context), ShortcutManagerCompat.getIconMaxHeight(context))
	}
	private var shortcutsUpdateJob: Job? = null

	init {
		settings.subscribe(this)
	}

	override fun onInvalidated(tables: Set<String>) {
		if (!settings.isDynamicShortcutsEnabled) {
			return
		}
		val prevJob = shortcutsUpdateJob
		shortcutsUpdateJob = processLifecycleScope.launch(Dispatchers.Default) {
			prevJob?.join()
			updateShortcutsImpl()
		}
	}

	override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
		if (key == AppSettings.KEY_SHORTCUTS) {
			if (settings.isDynamicShortcutsEnabled) {
				onInvalidated(emptySet())
			} else {
				clearShortcuts()
			}
		}
	}

	suspend fun requestPinShortcut(manga: Manga): Boolean {
		return ShortcutManagerCompat.requestPinShortcut(context, buildShortcutInfo(manga), null)
	}

	@VisibleForTesting
	suspend fun await(): Boolean {
		return shortcutsUpdateJob?.join() != null
	}

	fun isDynamicShortcutsAvailable(): Boolean {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1
			&& context.getSystemService(ShortcutManager::class.java).maxShortcutCountPerActivity > 0
	}

	fun notifyMangaOpened(mangaId: Long) {
		ShortcutManagerCompat.reportShortcutUsed(context, mangaId.toString())
	}

	private suspend fun updateShortcutsImpl() = runCatchingCancellable {
		val shortcuts = historyRepository.getList(0, ShortcutManagerCompat.getMaxShortcutCountPerActivity(context))
			.filter { x -> x.title.isNotEmpty() }
			.map { buildShortcutInfo(it) }
		ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
	}.onFailure {
		it.printStackTraceDebug()
	}

	private fun clearShortcuts() {
		try {
			ShortcutManagerCompat.removeAllDynamicShortcuts(context)
		} catch (_: IllegalStateException) {
		}
	}

	private suspend fun buildShortcutInfo(manga: Manga): ShortcutInfoCompat {
		val icon = runCatchingCancellable {
			val (width, height) = iconWidthAndHeight
			coil.execute(
				ImageRequest.Builder(context)
					.data(manga.coverUrl)
					.size(width, height)
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
			.build()
	}
}
