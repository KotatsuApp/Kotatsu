package org.koitharu.kotatsu.core.parser

import okhttp3.Headers
import org.koitharu.kotatsu.base.domain.MangaLoaderContext
import org.koitharu.kotatsu.core.model.*
import org.koitharu.kotatsu.core.network.CommonHeaders

abstract class RemoteMangaRepository(
	protected val loaderContext: MangaLoaderContext
) : MangaRepository {

	protected abstract val source: MangaSource

	protected val conf by lazy {
		loaderContext.getSettings(source)
	}

	override val sortOrders: Set<SortOrder> get() = emptySet()

	override suspend fun getPageRequest(page: MangaPage): RequestDraft {
		return RequestDraft(
			url = page.url,
			headers = Headers.headersOf(CommonHeaders.REFERER, page.referer)
		)
	}

	override suspend fun getTags(): Set<MangaTag> = emptySet()

	abstract fun onCreatePreferences(): Set<String>
}