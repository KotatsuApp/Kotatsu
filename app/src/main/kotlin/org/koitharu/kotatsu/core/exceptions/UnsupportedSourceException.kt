package org.koitharu.kotatsu.core.exceptions

import org.koitharu.kotatsu.parsers.model.Manga

class UnsupportedSourceException(
	message: String?,
	val manga: Manga?,
) : IllegalArgumentException(message)
