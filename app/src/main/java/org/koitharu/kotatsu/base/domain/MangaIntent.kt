package org.koitharu.kotatsu.base.domain

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import org.koitharu.kotatsu.core.model.parcelable.ParcelableManga
import org.koitharu.kotatsu.parsers.model.Manga

class MangaIntent private constructor(
	val manga: Manga?,
	val mangaId: Long,
	val uri: Uri?,
) {

	constructor(intent: Intent?) : this(
		manga = intent?.getParcelableExtra<ParcelableManga>(KEY_MANGA)?.manga,
		mangaId = intent?.getLongExtra(KEY_ID, ID_NONE) ?: ID_NONE,
		uri = intent?.data
	)

	constructor(args: Bundle?) : this(
		manga = args?.getParcelable<ParcelableManga>(KEY_MANGA)?.manga,
		mangaId = args?.getLong(KEY_ID, ID_NONE) ?: ID_NONE,
		uri = null
	)

	companion object {

		const val ID_NONE = 0L

		const val KEY_MANGA = "manga"
		const val KEY_ID = "id"
	}
}
