package org.koitharu.kotatsu.list.ui.model

import org.koitharu.kotatsu.base.ui.widgets.ChipsView
import org.koitharu.kotatsu.parsers.model.SortOrder

data class ListHeader2(
	val chips: Collection<ChipsView.ChipModel>,
	val sortOrder: SortOrder?,
) : ListModel