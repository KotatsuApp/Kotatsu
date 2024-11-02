package org.koitharu.kotatsu.local.ui

import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.core.ui.CoroutineIntentService
import org.koitharu.kotatsu.local.data.index.LocalMangaIndex
import javax.inject.Inject

@AndroidEntryPoint
class LocalIndexUpdateService : CoroutineIntentService() {

	@Inject
	lateinit var localMangaIndex: LocalMangaIndex

	override suspend fun IntentJobContext.processIntent(intent: Intent) {
		localMangaIndex.update()
	}

	override fun IntentJobContext.onError(error: Throwable) = Unit
}
