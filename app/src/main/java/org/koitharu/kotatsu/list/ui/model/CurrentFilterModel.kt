package org.koitharu.kotatsu.list.ui.model

import org.koitharu.kotatsu.base.ui.widgets.ChipsView

data class CurrentFilterModel(
	val chips: Collection<ChipsView.ChipModel>,
) : ListModel