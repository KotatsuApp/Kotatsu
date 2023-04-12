package org.koitharu.kotatsu.base.domain

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import org.koitharu.kotatsu.base.ui.BaseActivity
import org.koitharu.kotatsu.core.model.parcelable.ParcelableManga
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.utils.ext.getParcelableCompat
import org.koitharu.kotatsu.utils.ext.getParcelableExtraCompat

class MangaIntent private constructor(
	val manga: Manga?,
	val mangaId: Long,
	val uri: Uri?,
) {

	constructor(intent: Intent?) : this(
		manga = intent?.getParcelableExtraCompat<ParcelableManga>(KEY_MANGA)?.manga,
		mangaId = intent?.getLongExtra(KEY_ID, ID_NONE) ?: ID_NONE,
		uri = intent?.data,
	)

	constructor(savedStateHandle: SavedStateHandle) : this(
		manga = savedStateHandle.get<ParcelableManga>(KEY_MANGA)?.manga,
		mangaId = savedStateHandle[KEY_ID] ?: ID_NONE,
		uri = savedStateHandle[BaseActivity.EXTRA_DATA],
	)

	constructor(args: Bundle?) : this(
		manga = args?.getParcelableCompat<ParcelableManga>(KEY_MANGA)?.manga,
		mangaId = args?.getLong(KEY_ID, ID_NONE) ?: ID_NONE,
		uri = null,
	)

	companion object {

		const val ID_NONE = 0L

		const val KEY_MANGA = "manga"
		const val KEY_ID = "id"
	}
}
