package org.koitharu.kotatsu.ui.settings.sources

import android.view.ViewGroup
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.ui.common.list.BaseViewHolder

class SourceDividerHolder(parent: ViewGroup) :
	BaseViewHolder<Unit, Unit>(parent, R.layout.item_sources_pref_divider) {

	override fun onBind(data: Unit, extra: Unit) = Unit
}