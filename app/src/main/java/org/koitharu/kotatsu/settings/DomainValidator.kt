package org.koitharu.kotatsu.settings

import okhttp3.internal.toCanonicalHost
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.utils.EditTextValidator

class DomainValidator : EditTextValidator() {

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

	private fun checkCharacters(value: String): Boolean {
		for (i in value.indices) {
			val c = value[i]
			if (c !in '\u0020'..'\u007e') {
				return false
			}
		}
		return true
	}
}
