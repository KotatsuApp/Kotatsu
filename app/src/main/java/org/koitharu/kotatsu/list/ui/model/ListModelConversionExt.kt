package org.koitharu.kotatsu.list.ui.model

import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.exceptions.resolve.ResolvableException
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.prefs.ListMode
import kotlin.math.roundToInt

fun Manga.toListModel() = MangaListModel(
	id = id,
	title = title,
	subtitle = tags.joinToString(", ") { it.title },
	coverUrl = coverUrl,
	manga = this
)

fun Manga.toListDetailedModel() = MangaListDetailedModel(
	id = id,
	title = title,
	subtitle = altTitle,
	rating = "${(rating * 10).roundToInt()}/10",
	tags = tags.joinToString(", ") { it.title },
	coverUrl = coverUrl,
	manga = this
)

fun Manga.toGridModel() = MangaGridModel(
	id = id,
	title = title,
	coverUrl = coverUrl,
	manga = this
)

fun List<Manga>.toUi(mode: ListMode): List<ListModel> = when (mode) {
	ListMode.LIST -> map(Manga::toListModel)
	ListMode.DETAILED_LIST -> map(Manga::toListDetailedModel)
	ListMode.GRID -> map(Manga::toGridModel)
}

fun <C : MutableCollection<ListModel>> List<Manga>.toUi(destination: C, mode: ListMode): C =
	when (mode) {
		ListMode.LIST -> mapTo(destination, Manga::toListModel)
		ListMode.DETAILED_LIST -> mapTo(destination, Manga::toListDetailedModel)
		ListMode.GRID -> mapTo(destination, Manga::toGridModel)
	}

fun Throwable.toErrorState(canRetry: Boolean = true) = ErrorState(
	exception = this,
	icon = R.drawable.ic_error_large,
	canRetry = canRetry,
	buttonText = (this as? ResolvableException)?.resolveTextId ?: R.string.try_again
)

fun Throwable.toErrorFooter() = ErrorFooter(
	exception = this,
	icon = R.drawable.ic_alert_outline
)