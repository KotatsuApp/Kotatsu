package org.koitharu.kotatsu.core.parser

import android.net.Uri
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
		val uri = Uri.parse(url)
		val path = uri.path ?: error("Cannot generate uid: bad uri \"$url\"")
		val x = source.name.hashCode()
		val y = path.hashCode()
		return (x.toLong() shl 32) or (y.toLong() and 0xffffffffL)
	}

	protected fun generateUid(id: Int): Long {
		val x = source.name.hashCode()
		return (x.toLong() shl 32) or (id.toLong() and 0xffffffffL)
	}
}