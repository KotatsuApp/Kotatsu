package org.koitharu.kotatsu.list.ui.model

import java.net.SocketTimeoutException
import java.net.UnknownHostException
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.exceptions.CloudFlareProtectedException
import org.koitharu.kotatsu.core.exceptions.resolve.ExceptionResolver
import org.koitharu.kotatsu.core.prefs.ListMode
import org.koitharu.kotatsu.history.domain.PROGRESS_NONE
import org.koitharu.kotatsu.list.domain.ListExtraProvider
import org.koitharu.kotatsu.parsers.exception.AuthRequiredException
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.utils.ext.ifZero

fun Manga.toListModel(counter: Int, progress: Float) = MangaListModel(
	id = id,
	title = title,
	subtitle = tags.joinToString(", ") { it.title },
	coverUrl = coverUrl,
	manga = this,
	counter = counter,
	progress = progress,
)

fun Manga.toListDetailedModel(counter: Int, progress: Float) = MangaListDetailedModel(
	id = id,
	title = title,
	subtitle = altTitle,
	rating = if (hasRating) String.format("%.1f", rating * 5) else null,
	tags = tags.joinToString(", ") { it.title },
	coverUrl = coverUrl,
	manga = this,
	counter = counter,
	progress = progress,
)

fun Manga.toGridModel(counter: Int, progress: Float) = MangaGridModel(
	id = id,
	title = title,
	coverUrl = coverUrl,
	manga = this,
	counter = counter,
	progress = progress,
)

suspend fun List<Manga>.toUi(
	mode: ListMode,
	extraProvider: ListExtraProvider,
): List<MangaItemModel> = toUi(ArrayList(size), mode, extraProvider)

fun List<Manga>.toUi(
	mode: ListMode,
): List<MangaItemModel> = toUi(ArrayList(size), mode)

fun <C : MutableCollection<in MangaItemModel>> List<Manga>.toUi(
	destination: C,
	mode: ListMode,
): C = when (mode) {
	ListMode.LIST -> mapTo(destination) { it.toListModel(0, PROGRESS_NONE) }
	ListMode.DETAILED_LIST -> mapTo(destination) { it.toListDetailedModel(0, PROGRESS_NONE) }
	ListMode.GRID -> mapTo(destination) { it.toGridModel(0, PROGRESS_NONE) }
}

suspend fun <C : MutableCollection<in MangaItemModel>> List<Manga>.toUi(
	destination: C,
	mode: ListMode,
	extraProvider: ListExtraProvider,
): C = when (mode) {
	ListMode.LIST -> mapTo(destination) {
		it.toListModel(extraProvider.getCounter(it.id), extraProvider.getProgress(it.id))
	}
	ListMode.DETAILED_LIST -> mapTo(destination) {
		it.toListDetailedModel(extraProvider.getCounter(it.id), extraProvider.getProgress(it.id))
	}
	ListMode.GRID -> mapTo(destination) {
		it.toGridModel(extraProvider.getCounter(it.id), extraProvider.getProgress(it.id))
	}
}

fun Throwable.toErrorState(canRetry: Boolean = true) = ErrorState(
	exception = this,
	icon = getErrorIcon(this),
	canRetry = canRetry,
	buttonText = ExceptionResolver.getResolveStringId(this).ifZero { R.string.try_again },
)

fun Throwable.toErrorFooter() = ErrorFooter(
	exception = this,
	icon = R.drawable.ic_alert_outline,
)

private fun getErrorIcon(error: Throwable) = when (error) {
	is AuthRequiredException -> R.drawable.ic_auth_key_large
	is CloudFlareProtectedException -> R.drawable.ic_bot_large
	is UnknownHostException,
	is SocketTimeoutException,
	-> R.drawable.ic_plug_large
	else -> R.drawable.ic_error_large
}
