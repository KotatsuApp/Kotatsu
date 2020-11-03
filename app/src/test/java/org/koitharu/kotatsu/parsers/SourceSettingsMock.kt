package org.koitharu.kotatsu.parsers

import org.koitharu.kotatsu.core.prefs.SourceSettings

class SourceSettingsMock : SourceSettings {

	override fun getDomain(defaultValue: String) = defaultValue

	override fun isUseSsl(defaultValue: Boolean) = defaultValue
}