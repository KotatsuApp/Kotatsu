package org.koitharu.kotatsu.core.parser

import android.net.Uri
import coil.map.Mapper
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.koitharu.kotatsu.parsers.model.MangaSource

class FaviconMapper() : Mapper<Uri, HttpUrl> {

	override fun map(data: Uri): HttpUrl {
		val mangaSource = MangaSource.valueOf(data.schemeSpecificPart)
		val repo = MangaRepository(mangaSource) as RemoteMangaRepository
		return repo.getFaviconUrl().toHttpUrl()
	}

	override fun handles(data: Uri) = data.scheme == "favicon"
}