package org.koitharu.kotatsu.base.domain

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import org.koitharu.kotatsu.core.model.Manga

data class MangaIntent(
	val manga: Manga?,
	val mangaId: Long,
	val uri: Uri?
) {

	companion object {

		fun from(intent: Intent?) = MangaIntent(
			manga = intent?.getParcelableExtra(KEY_MANGA),
			mangaId = intent?.getLongExtra(KEY_ID, ID_NONE) ?: ID_NONE,
			uri = intent?.data
		)

		fun from(args: Bundle?) = MangaIntent(
			manga = args?.getParcelable(KEY_MANGA),
			mangaId = args?.getLong(KEY_ID, ID_NONE) ?: ID_NONE,
			uri = null
		)

		const val ID_NONE = 0L

		const val KEY_MANGA = "manga"
		const val KEY_ID = "id"
	}
}