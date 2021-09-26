package org.koitharu.kotatsu.core.parser

import org.koitharu.kotatsu.core.prefs.SourceSettings

class SourceSettingsStub : SourceSettings {

	override fun getDomain(defaultValue: String) = defaultValue

	override fun isUseSsl(defaultValue: Boolean) = defaultValue
}