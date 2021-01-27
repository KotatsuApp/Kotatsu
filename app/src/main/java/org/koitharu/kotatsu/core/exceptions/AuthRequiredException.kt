package org.koitharu.kotatsu.core.exceptions

import androidx.annotation.StringRes
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.exceptions.resolve.ResolvableException

class AuthRequiredException(
	val url: String
) : RuntimeException("Authorization required"), ResolvableException {

	@StringRes
	override val resolveTextId: Int = R.string.sign_in
}