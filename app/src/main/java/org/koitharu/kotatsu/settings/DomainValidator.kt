package org.koitharu.kotatsu.settings

import okhttp3.HttpUrl
import okhttp3.internal.toCanonicalHost
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.utils.EditTextValidator

class DomainValidator : EditTextValidator() {

	private val urlBuilder = HttpUrl.Builder()

	override fun validate(text: String): ValidationResult {
		val trimmed = text.trim()
		if (trimmed.isEmpty()) {
			return ValidationResult.Success
		}
		return if (!checkCharacters(trimmed) || trimmed.toCanonicalHost() == null) {
			ValidationResult.Failed(context.getString(R.string.invalid_domain_message))
		} else {
			ValidationResult.Success
		}
	}

	private fun checkCharacters(value: String): Boolean = runCatching {
		urlBuilder.host(value)
	}.isSuccess
}
