package org.koitharu.kotatsu.list.ui.model

import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.exceptions.CloudFlareProtectedException
import org.koitharu.kotatsu.core.exceptions.resolve.ExceptionResolver
import org.koitharu.kotatsu.core.prefs.ListMode
import org.koitharu.kotatsu.list.domain.CountersProvider
import org.koitharu.kotatsu.parsers.exception.AuthRequiredException
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.utils.ext.ifZero

fun Manga.toListModel(counter: Int) = MangaListModel(
	id = id,
	title = title,
	subtitle = tags.joinToString(", ") { it.title },
	coverUrl = coverUrl,
	manga = this,
	counter = counter,
)

fun Manga.toListDetailedModel(counter: Int) = MangaListDetailedModel(
	id = id,
	title = title,
	subtitle = altTitle,
	rating = if (rating == Manga.NO_RATING) null else String.format("%.1f", rating * 5),
	tags = tags.joinToString(", ") { it.title },
	coverUrl = coverUrl,
	manga = this,
	counter = counter,
)

fun Manga.toGridModel(counter: Int) = MangaGridModel(
	id = id,
	title = title,
	coverUrl = coverUrl,
	manga = this,
	counter = counter,
)

suspend fun List<Manga>.toUi(
	mode: ListMode,
	countersProvider: CountersProvider,
): List<ListModel> = when (mode) {
	ListMode.LIST -> map { it.toListModel(countersProvider.getCounter(it.id)) }
	ListMode.DETAILED_LIST -> map { it.toListDetailedModel(countersProvider.getCounter(it.id)) }
	ListMode.GRID -> map { it.toGridModel(countersProvider.getCounter(it.id)) }
}

suspend fun <C : MutableCollection<ListModel>> List<Manga>.toUi(
	destination: C,
	mode: ListMode,
	countersProvider: CountersProvider,
): C = when (mode) {
	ListMode.LIST -> mapTo(destination) { it.toListModel(countersProvider.getCounter(it.id)) }
	ListMode.DETAILED_LIST -> mapTo(destination) { it.toListDetailedModel(countersProvider.getCounter(it.id)) }
	ListMode.GRID -> mapTo(destination) { it.toGridModel(countersProvider.getCounter(it.id)) }
}

fun List<Manga>.toUi(
	mode: ListMode,
): List<ListModel> = when (mode) {
	ListMode.LIST -> map { it.toListModel(0) }
	ListMode.DETAILED_LIST -> map { it.toListDetailedModel(0) }
	ListMode.GRID -> map { it.toGridModel(0) }
}

fun <C : MutableCollection<ListModel>> List<Manga>.toUi(
	destination: C,
	mode: ListMode,
): C = when (mode) {
	ListMode.LIST -> mapTo(destination) { it.toListModel(0) }
	ListMode.DETAILED_LIST -> mapTo(destination) { it.toListDetailedModel(0) }
	ListMode.GRID -> mapTo(destination) { it.toGridModel(0) }
}

fun Throwable.toErrorState(canRetry: Boolean = true) = ErrorState(
	exception = this,
	icon = getErrorIcon(this),
	canRetry = canRetry,
	buttonText = ExceptionResolver.getResolveStringId(this).ifZero { R.string.try_again }
)

fun Throwable.toErrorFooter() = ErrorFooter(
	exception = this,
	icon = R.drawable.ic_alert_outline
)

private fun getErrorIcon(error: Throwable) = when (error) {
	is AuthRequiredException,
	is CloudFlareProtectedException -> R.drawable.ic_denied_large
	else -> R.drawable.ic_error_large
}