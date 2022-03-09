package org.koitharu.kotatsu.core.exceptions

import androidx.annotation.StringRes
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.exceptions.resolve.ResolvableException
import org.koitharu.kotatsu.core.model.MangaSource

class AuthRequiredException(
	val source: MangaSource,
) : RuntimeException("Authorization required"), ResolvableException {

	@StringRes
	override val resolveTextId: Int = R.string.sign_in
}