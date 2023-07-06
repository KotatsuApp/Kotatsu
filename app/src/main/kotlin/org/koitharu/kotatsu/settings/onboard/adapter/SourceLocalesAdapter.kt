package org.koitharu.kotatsu.settings.onboard.adapter

import org.koitharu.kotatsu.core.ui.BaseListAdapter
import org.koitharu.kotatsu.settings.onboard.model.SourceLocale

class SourceLocalesAdapter(
	listener: SourceLocaleListener,
) : BaseListAdapter<SourceLocale>() {

	init {
		delegatesManager.addDelegate(sourceLocaleAD(listener))
	}
}
