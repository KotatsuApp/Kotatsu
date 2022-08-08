package org.koitharu.kotatsu.core.parser

import android.net.Uri
import coil.map.Mapper
import coil.request.Options
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.koitharu.kotatsu.core.model.MangaSource

class FaviconMapper : Mapper<Uri, HttpUrl> {

	override fun map(data: Uri, options: Options): HttpUrl? {
		if (data.scheme != "favicon") {
			return null
		}
		val mangaSource = MangaSource(data.schemeSpecificPart) ?: return null
		val repo = MangaRepository(mangaSource) as RemoteMangaRepository
		return repo.getFaviconUrl().toHttpUrl()
	}
}