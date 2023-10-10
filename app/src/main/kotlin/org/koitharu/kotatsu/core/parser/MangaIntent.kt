package org.koitharu.kotatsu.core.parser

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import org.koitharu.kotatsu.core.model.parcelable.ParcelableManga
import org.koitharu.kotatsu.core.ui.BaseActivity
import org.koitharu.kotatsu.core.util.ext.getParcelableCompat
import org.koitharu.kotatsu.core.util.ext.getParcelableExtraCompat
import org.koitharu.kotatsu.parsers.model.Manga

class MangaIntent private constructor(
	@JvmField val manga: Manga?,
	@JvmField val id: Long,
	@JvmField val uri: Uri?,
) {

	constructor(intent: Intent?) : this(
		manga = intent?.getParcelableExtraCompat<ParcelableManga>(KEY_MANGA)?.manga,
		id = intent?.getLongExtra(KEY_ID, ID_NONE) ?: ID_NONE,
		uri = intent?.data,
	)

	constructor(savedStateHandle: SavedStateHandle) : this(
		manga = savedStateHandle.get<ParcelableManga>(KEY_MANGA)?.manga,
		id = savedStateHandle[KEY_ID] ?: ID_NONE,
		uri = savedStateHandle[BaseActivity.EXTRA_DATA],
	)

	constructor(args: Bundle?) : this(
		manga = args?.getParcelableCompat<ParcelableManga>(KEY_MANGA)?.manga,
		id = args?.getLong(KEY_ID, ID_NONE) ?: ID_NONE,
		uri = null,
	)

	val mangaId: Long
		get() = if (id != ID_NONE) id else manga?.id ?: uri?.lastPathSegment?.toLongOrNull() ?: ID_NONE

	companion object {

		const val ID_NONE = 0L

		const val KEY_MANGA = "manga"
		const val KEY_ID = "id"

		fun of(manga: Manga) = MangaIntent(manga, manga.id, null)
	}
}
