package org.koitharu.kotatsu.details.domain

import android.text.Html
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import androidx.core.text.getSpans
import androidx.core.text.parseAsHtml
import coil3.request.CachePolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import okio.IOException
import org.koitharu.kotatsu.core.model.isLocal
import org.koitharu.kotatsu.core.nav.MangaIntent
import org.koitharu.kotatsu.core.os.NetworkState
import org.koitharu.kotatsu.core.parser.CachingMangaRepository
import org.koitharu.kotatsu.core.parser.MangaDataRepository
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.util.ext.peek
import org.koitharu.kotatsu.core.util.ext.printStackTraceDebug
import org.koitharu.kotatsu.core.util.ext.sanitize
import org.koitharu.kotatsu.details.data.MangaDetails
import org.koitharu.kotatsu.explore.domain.RecoverMangaUseCase
import org.koitharu.kotatsu.local.data.LocalMangaRepository
import org.koitharu.kotatsu.parsers.exception.NotFoundException
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.util.recoverNotNull
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
import org.koitharu.kotatsu.tracker.domain.CheckNewChaptersUseCase
import javax.inject.Inject
import javax.inject.Provider

class DetailsLoadUseCase @Inject constructor(
	private val mangaDataRepository: MangaDataRepository,
	private val localMangaRepository: LocalMangaRepository,
	private val mangaRepositoryFactory: MangaRepository.Factory,
	private val recoverUseCase: RecoverMangaUseCase,
	private val imageGetter: Html.ImageGetter,
	private val newChaptersUseCaseProvider: Provider<CheckNewChaptersUseCase>,
	private val networkState: NetworkState,
) {

	operator fun invoke(intent: MangaIntent, force: Boolean): Flow<MangaDetails> = channelFlow {
		val manga = requireNotNull(mangaDataRepository.resolveIntent(intent, withChapters = true)) {
			"Cannot resolve intent $intent"
		}
		val override = mangaDataRepository.getOverride(manga.id)
		send(
			MangaDetails(
				manga = manga,
				localManga = null,
				override = override,
				description = null,
				isLoaded = false,
			),
		)
		if (manga.isLocal) {
			val details = getDetails(manga, force)
			send(
				MangaDetails(
					manga = details,
					localManga = null,
					override = override,
					description = details.description?.parseAsHtml(withImages = false)?.trim(),
					isLoaded = true,
				),
			)
			return@channelFlow
		}
		val local = async {
			localMangaRepository.findSavedManga(manga)
		}
		if (!force && networkState.isOfflineOrRestricted()) {
			// try to avoid loading if has saved manga
			val localManga = local.await()
			if (localManga != null) {
				send(
					MangaDetails(
						manga = manga,
						localManga = localManga,
						override = override,
						description = manga.description?.parseAsHtml(withImages = true)?.trim(),
						isLoaded = true,
					),
				)
				return@channelFlow
			}
		}
		try {
			val details = getDetails(manga, force)
			launch { mangaDataRepository.updateChapters(details) }
			launch { updateTracker(details) }
			send(
				MangaDetails(
					manga = details,
					localManga = local.peek(),
					override = override,
					description = details.description?.parseAsHtml(withImages = false)?.trim(),
					isLoaded = false,
				),
			)
			send(
				MangaDetails(
					manga = details,
					localManga = local.await(),
					override = override,
					description = details.description?.parseAsHtml(withImages = true)?.trim(),
					isLoaded = true,
				),
			)
		} catch (e: IOException) {
			local.await()?.manga?.also { localManga ->
				send(
					MangaDetails(
						manga = localManga,
						localManga = null,
						override = override,
						description = localManga.description?.parseAsHtml(withImages = false)?.trim(),
						isLoaded = true,
					),
				)
			} ?: close(e)
		}
	}

	private suspend fun getDetails(seed: Manga, force: Boolean) = runCatchingCancellable {
		val repository = mangaRepositoryFactory.create(seed.source)
		if (repository is CachingMangaRepository) {
			repository.getDetails(seed, if (force) CachePolicy.WRITE_ONLY else CachePolicy.ENABLED)
		} else {
			repository.getDetails(seed)
		}
	}.recoverNotNull { e ->
		if (e is NotFoundException) {
			recoverUseCase(seed)
		} else {
			null
		}
	}.getOrThrow()

	private suspend fun String.parseAsHtml(withImages: Boolean): CharSequence? {
		return if (withImages) {
			runInterruptible(Dispatchers.IO) {
				parseAsHtml(imageGetter = imageGetter)
			}.filterSpans()
		} else {
			runInterruptible(Dispatchers.Default) {
				parseAsHtml()
			}.filterSpans().sanitize()
		}.takeUnless { it.isBlank() }
	}

	private fun Spanned.filterSpans(): Spanned {
		val spannable = SpannableString.valueOf(this)
		val spans = spannable.getSpans<ForegroundColorSpan>()
		for (span in spans) {
			spannable.removeSpan(span)
		}
		return spannable
	}

	private suspend fun updateTracker(details: Manga) = runCatchingCancellable {
		newChaptersUseCaseProvider.get()(details)
	}.onFailure { e ->
		e.printStackTraceDebug()
	}
}
