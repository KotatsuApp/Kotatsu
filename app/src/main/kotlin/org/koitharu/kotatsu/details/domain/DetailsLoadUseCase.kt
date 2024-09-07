package org.koitharu.kotatsu.details.domain

import android.text.Html
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import androidx.core.text.getSpans
import androidx.core.text.parseAsHtml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import okio.IOException
import org.koitharu.kotatsu.core.model.isLocal
import org.koitharu.kotatsu.core.parser.MangaDataRepository
import org.koitharu.kotatsu.core.parser.MangaIntent
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
) {

	operator fun invoke(intent: MangaIntent): Flow<MangaDetails> = channelFlow {
		val manga = requireNotNull(mangaDataRepository.resolveIntent(intent)) {
			"Cannot resolve intent $intent"
		}
		val local = if (!manga.isLocal) {
			async {
				localMangaRepository.findSavedManga(manga)
			}
		} else {
			null
		}
		send(MangaDetails(manga, null, null, false))
		try {
			val details = getDetails(manga)
			launch { updateTracker(details) }
			send(
				MangaDetails(
					details,
					local?.peek(),
					details.description?.parseAsHtml(withImages = false)?.trim(),
					false,
				),
			)
			send(
				MangaDetails(
					details,
					local?.await(),
					details.description?.parseAsHtml(withImages = true)?.trim(),
					true,
				),
			)
		} catch (e: IOException) {
			local?.await()?.manga?.also { localManga ->
				send(
					MangaDetails(
						localManga,
						null,
						localManga.description?.parseAsHtml(withImages = false)?.trim(),
						true,
					),
				)
			} ?: close(e)
		}
	}

	private suspend fun getDetails(seed: Manga) = runCatchingCancellable {
		val repository = mangaRepositoryFactory.create(seed.source)
		repository.getDetails(seed)
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
