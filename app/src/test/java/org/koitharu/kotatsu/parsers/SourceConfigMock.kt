package org.koitharu.kotatsu.parsers

import org.koitharu.kotatsu.core.prefs.SourceConfig

class SourceConfigMock : SourceConfig {

	override fun getDomain(defaultValue: String) = defaultValue

	override fun isUseSsl(defaultValue: Boolean) = defaultValue
}