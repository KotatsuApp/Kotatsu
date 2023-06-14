package org.koitharu.kotatsu.list.ui.model

import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.exceptions.CloudFlareProtectedException
import org.koitharu.kotatsu.core.exceptions.resolve.ExceptionResolver
import org.koitharu.kotatsu.core.prefs.ListMode
import org.koitharu.kotatsu.core.ui.widgets.ChipsView
import org.koitharu.kotatsu.core.util.ext.ifZero
import org.koitharu.kotatsu.history.data.PROGRESS_NONE
import org.koitharu.kotatsu.list.domain.ListExtraProvider
import org.koitharu.kotatsu.parsers.exception.AuthRequiredException
import org.koitharu.kotatsu.parsers.model.Manga
import java.net.SocketTimeoutException
import java.net.UnknownHostException

suspend fun Manga.toListModel(
	extraProvider: ListExtraProvider?
) = MangaListModel(
	id = id,
	title = title,
	subtitle = tags.joinToString(", ") { it.title },
	coverUrl = coverUrl,
	manga = this,
	counter = extraProvider?.getCounter(id) ?: 0,
	progress = extraProvider?.getProgress(id) ?: PROGRESS_NONE,
)

suspend fun Manga.toListDetailedModel(
	extraProvider: ListExtraProvider?,
) = MangaListDetailedModel(
	id = id,
	title = title,
	subtitle = altTitle,
	coverUrl = coverUrl,
	manga = this,
	counter = extraProvider?.getCounter(id) ?: 0,
	progress = extraProvider?.getProgress(id) ?: PROGRESS_NONE,
	tags = tags.map {
		ChipsView.ChipModel(
			tint = extraProvider?.getTagTint(it) ?: 0,
			title = it.title,
			icon = 0,
			isCheckable = false,
			isChecked = false,
			data = it,
		)
	},
)

suspend fun Manga.toGridModel(
	extraProvider: ListExtraProvider?,
) = MangaGridModel(
	id = id,
	title = title,
	coverUrl = coverUrl,
	manga = this,
	counter = extraProvider?.getCounter(id) ?: 0,
	progress = extraProvider?.getProgress(id) ?: PROGRESS_NONE,
)

suspend fun List<Manga>.toUi(
	mode: ListMode,
	extraProvider: ListExtraProvider,
): List<MangaItemModel> = toUi(ArrayList(size), mode, extraProvider)

suspend fun <C : MutableCollection<in MangaItemModel>> List<Manga>.toUi(
	destination: C,
	mode: ListMode,
	extraProvider: ListExtraProvider,
): C = when (mode) {
	ListMode.LIST -> mapTo(destination) { it.toListModel(extraProvider) }
	ListMode.DETAILED_LIST -> mapTo(destination) { it.toListDetailedModel(extraProvider) }
	ListMode.GRID -> mapTo(destination) { it.toGridModel(extraProvider) }
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
