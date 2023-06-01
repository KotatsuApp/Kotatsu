package org.koitharu.kotatsu.list.ui.model

import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.exceptions.CloudFlareProtectedException
import org.koitharu.kotatsu.core.exceptions.resolve.ExceptionResolver
import org.koitharu.kotatsu.core.parser.MangaTagHighlighter
import org.koitharu.kotatsu.core.prefs.ListMode
import org.koitharu.kotatsu.core.ui.widgets.ChipsView
import org.koitharu.kotatsu.core.util.ext.ifZero
import org.koitharu.kotatsu.history.data.PROGRESS_NONE
import org.koitharu.kotatsu.list.domain.ListExtraProvider
import org.koitharu.kotatsu.parsers.exception.AuthRequiredException
import org.koitharu.kotatsu.parsers.model.Manga
import java.net.SocketTimeoutException
import java.net.UnknownHostException

fun Manga.toListModel(
	counter: Int,
	progress: Float,
) = MangaListModel(
	id = id,
	title = title,
	subtitle = tags.joinToString(", ") { it.title },
	coverUrl = coverUrl,
	manga = this,
	counter = counter,
	progress = progress,
)

fun Manga.toListDetailedModel(
	counter: Int,
	progress: Float,
	tagHighlighter: MangaTagHighlighter?,
) = MangaListDetailedModel(
	id = id,
	title = title,
	subtitle = altTitle,
	coverUrl = coverUrl,
	manga = this,
	counter = counter,
	progress = progress,
	tags = tags.map {
		ChipsView.ChipModel(
			tint = tagHighlighter?.getTint(it) ?: 0,
			title = it.title,
			icon = 0,
			isCheckable = false,
			isChecked = false,
			data = it,
		)
	},
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
	tagHighlighter: MangaTagHighlighter?,
): List<MangaItemModel> = toUi(ArrayList(size), mode, extraProvider, tagHighlighter)

fun List<Manga>.toUi(
	mode: ListMode,
	tagHighlighter: MangaTagHighlighter?,
): List<MangaItemModel> = toUi(ArrayList(size), mode, tagHighlighter)

fun <C : MutableCollection<in MangaItemModel>> List<Manga>.toUi(
	destination: C,
	mode: ListMode,
	tagHighlighter: MangaTagHighlighter?,
): C = when (mode) {
	ListMode.LIST -> mapTo(destination) { it.toListModel(0, PROGRESS_NONE) }
	ListMode.DETAILED_LIST -> mapTo(destination) { it.toListDetailedModel(0, PROGRESS_NONE, tagHighlighter) }
	ListMode.GRID -> mapTo(destination) { it.toGridModel(0, PROGRESS_NONE) }
}

suspend fun <C : MutableCollection<in MangaItemModel>> List<Manga>.toUi(
	destination: C,
	mode: ListMode,
	extraProvider: ListExtraProvider,
	tagHighlighter: MangaTagHighlighter?,
): C = when (mode) {
	ListMode.LIST -> mapTo(destination) {
		it.toListModel(extraProvider.getCounter(it.id), extraProvider.getProgress(it.id))
	}

	ListMode.DETAILED_LIST -> mapTo(destination) {
		it.toListDetailedModel(extraProvider.getCounter(it.id), extraProvider.getProgress(it.id), tagHighlighter)
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
