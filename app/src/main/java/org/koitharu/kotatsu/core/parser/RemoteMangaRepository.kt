package org.koitharu.kotatsu.core.parser

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.koitharu.kotatsu.base.domain.MangaLoaderContext
import org.koitharu.kotatsu.core.model.MangaPage
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.core.model.MangaTag
import org.koitharu.kotatsu.core.model.SortOrder

abstract class RemoteMangaRepository(
	protected val loaderContext: MangaLoaderContext
) : MangaRepository {

	protected abstract val source: MangaSource

	protected val conf by lazy {
		loaderContext.getSettings(source)
	}

	override val sortOrders: Set<SortOrder> get() = emptySet()

	override suspend fun getPageUrl(page: MangaPage): String = page.url

	override suspend fun getTags(): Set<MangaTag> = emptySet()

	abstract fun onCreatePreferences(): Set<String>

	protected fun generateUid(url: String): Long {
		val uri = url.toHttpUrl()
		val x = source.name.hashCode()
		val y = "${uri.encodedPath}?${uri.query}".hashCode()
		return (x.toLong() shl 32) or (y.toLong() and 0xffffffffL)
	}

	protected fun generateUid(id: Int): Long {
		val x = source.name.hashCode()
		return (x.toLong() shl 32) or (id.toLong() and 0xffffffffL)
	}
}