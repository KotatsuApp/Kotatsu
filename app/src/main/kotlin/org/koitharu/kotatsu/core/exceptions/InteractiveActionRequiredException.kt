package org.koitharu.kotatsu.core.exceptions

import okio.IOException
import org.koitharu.kotatsu.parsers.model.MangaSource

class InteractiveActionRequiredException(
	val source: MangaSource,
	val url: String,
) : IOException("Interactive action is required for ${source.name}")
