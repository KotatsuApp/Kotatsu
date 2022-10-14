package org.koitharu.kotatsu.settings.onboard.adapter

import org.koitharu.kotatsu.settings.onboard.model.SourceLocale

interface SourceLocaleListener {

	fun onItemCheckedChanged(item: SourceLocale, isChecked: Boolean)
}
