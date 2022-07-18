package org.koitharu.kotatsu.settings

import okhttp3.internal.toCanonicalHost
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.utils.EditTextValidator

class DomainValidator : EditTextValidator() {

	override fun validate(text: String): ValidationResult {
		if (text.isBlank()) {
			return ValidationResult.Success
		}
		val host = text.trim().toCanonicalHost()
		return if (host == null) {
			ValidationResult.Failed(context.getString(R.string.invalid_domain_message))
		} else {
			ValidationResult.Success
		}
	}
}