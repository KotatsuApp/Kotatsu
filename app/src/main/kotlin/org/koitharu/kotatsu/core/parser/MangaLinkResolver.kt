package org.koitharu.kotatsu.core.parser

import android.net.Uri
import coil.request.CachePolicy
import dagger.Reusable
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.core.model.UnknownMangaSource
import org.koitharu.kotatsu.core.model.isNsfw
import org.koitharu.kotatsu.core.util.ext.ifNullOrEmpty
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.exception.NotFoundException
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaListFilter
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.util.almostEquals
import org.koitharu.kotatsu.parsers.util.levenshteinDistance
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
import javax.inject.Inject

@Reusable
class MangaLinkResolver @Inject constructor(
	private val repositoryFactory: MangaRepository.Factory,
	private val dataRepository: MangaDataRepository,
	private val context: MangaLoaderContext,
) {

	suspend fun resolve(uri: Uri): Manga {
		return if (uri.scheme == "kotatsu" || uri.host == "kotatsu.app") {
			resolveAppLink(uri)
		} else {
			resolveExternalLink(uri.toString())
		} ?: throw NotFoundException("Cannot resolve link", uri.toString())
	}

	private suspend fun resolveAppLink(uri: Uri): Manga? {
		require(uri.pathSegments.singleOrNull() == "manga") { "Invalid url" }
		val sourceName = requireNotNull(uri.getQueryParameter("source")) { "Source is not specified" }
		val source = MangaSource(sourceName)
		require(source != UnknownMangaSource) { "Manga source $sourceName is not supported" }
		val repo = repositoryFactory.create(source)
		return repo.findExact(
			url = uri.getQueryParameter("url"),
			title = uri.getQueryParameter("name"),
		)
	}

	private suspend fun resolveExternalLink(uri: String): Manga? {
		dataRepository.findMangaByPublicUrl(uri)?.let {
			return it
		}
		return context.newLinkResolver(uri).getManga()
	}

	private suspend fun MangaRepository.findExact(url: String?, title: String?): Manga? {
		if (!title.isNullOrEmpty()) {
			val list = getList(0, null, MangaListFilter(query = title))
			if (url != null) {
				list.find { it.url == url }?.let {
					return it
				}
			}
			list.minByOrNull { it.title.levenshteinDistance(title) }
				?.takeIf { it.title.almostEquals(title, 0.2f) }
				?.let { return it }
		}
		val seed = getDetailsNoCache(
			getSeedManga(source, url ?: return null, title),
		)
		return runCatchingCancellable {
			val seedTitle = seed.title.ifEmpty {
				seed.altTitle
			}.ifNullOrEmpty {
				seed.author
			} ?: return@runCatchingCancellable null
			val seedList = getList(0, null, MangaListFilter(query = seedTitle))
			seedList.first { x -> x.url == url }
		}.getOrThrow()
	}

	private suspend fun MangaRepository.getDetailsNoCache(manga: Manga): Manga = if (this is CachingMangaRepository) {
		getDetails(manga, CachePolicy.READ_ONLY)
	} else {
		getDetails(manga)
	}

	private fun getSeedManga(source: MangaSource, url: String, title: String?) = Manga(
		id = run {
			var h = 1125899906842597L
			source.name.forEach { c ->
				h = 31 * h + c.code
			}
			url.forEach { c ->
				h = 31 * h + c.code
			}
			h
		},
		title = title.orEmpty(),
		altTitle = null,
		url = url,
		publicUrl = "",
		rating = 0.0f,
		isNsfw = source.isNsfw(),
		coverUrl = "",
		tags = emptySet(),
		state = null,
		author = null,
		largeCoverUrl = null,
		description = null,
		chapters = null,
		source = source,
	)
}
