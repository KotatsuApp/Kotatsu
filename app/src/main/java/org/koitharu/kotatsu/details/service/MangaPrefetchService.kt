package org.koitharu.kotatsu.details.service

import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.coroutineScope
import org.koitharu.kotatsu.base.ui.CoroutineIntentService
import org.koitharu.kotatsu.core.cache.ContentCache
import org.koitharu.kotatsu.core.model.parcelable.ParcelableManga
import org.koitharu.kotatsu.core.model.parcelable.ParcelableMangaChapters
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.utils.ext.getParcelableExtraCompat
import org.koitharu.kotatsu.utils.ext.runCatchingCancellable
import javax.inject.Inject

@AndroidEntryPoint
class MangaPrefetchService : CoroutineIntentService() {

	@Inject
	lateinit var mangaRepositoryFactory: MangaRepository.Factory

	@Inject
	lateinit var cache: ContentCache

	override suspend fun processIntent(startId: Int, intent: Intent) {
		when (intent.action) {
			ACTION_PREFETCH_DETAILS -> prefetchDetails(
				manga = intent.getParcelableExtraCompat<ParcelableManga>(EXTRA_MANGA)?.manga ?: return,
			)

			ACTION_PREFETCH_PAGES -> prefetchPages(
				chapter = intent.getParcelableExtraCompat<ParcelableMangaChapters>(EXTRA_CHAPTER)
					?.chapters?.singleOrNull() ?: return,
			)
		}
	}

	override fun onError(startId: Int, error: Throwable) = Unit

	private suspend fun prefetchDetails(manga: Manga) = coroutineScope {
		val source = mangaRepositoryFactory.create(manga.source)
		runCatchingCancellable { source.getDetails(manga) }
	}

	private suspend fun prefetchPages(chapter: MangaChapter) {
		val source = mangaRepositoryFactory.create(chapter.source)
		runCatchingCancellable { source.getPages(chapter) }
	}

	companion object {

		private const val EXTRA_MANGA = "manga"
		private const val EXTRA_CHAPTER = "manga"
		private const val ACTION_PREFETCH_DETAILS = "details"
		private const val ACTION_PREFETCH_PAGES = "pages"

		fun prefetchDetails(context: Context, manga: Manga) {
			if (!isPrefetchAvailable(context, manga.source)) return
			val intent = Intent(context, MangaPrefetchService::class.java)
			intent.action = ACTION_PREFETCH_DETAILS
			intent.putExtra(EXTRA_MANGA, ParcelableManga(manga, withChapters = false))
			context.startService(intent)
		}

		fun prefetchPages(context: Context, chapter: MangaChapter) {
			if (!isPrefetchAvailable(context, chapter.source)) return
			val intent = Intent(context, MangaPrefetchService::class.java)
			intent.action = ACTION_PREFETCH_PAGES
			intent.putExtra(EXTRA_CHAPTER, ParcelableMangaChapters(listOf(chapter)))
			context.startService(intent)
		}

		private fun isPrefetchAvailable(context: Context, source: MangaSource): Boolean {
			if (source == MangaSource.LOCAL) {
				return false
			}
			val entryPoint = EntryPointAccessors.fromApplication(context, PrefetchCompanionEntryPoint::class.java)
			return entryPoint.contentCache.isCachingEnabled && entryPoint.settings.isContentPrefetchEnabled()
		}
	}
}
