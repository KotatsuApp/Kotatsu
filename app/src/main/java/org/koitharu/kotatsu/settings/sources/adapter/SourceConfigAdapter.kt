package org.koitharu.kotatsu.settings.sources.adapter

import com.hannesdorfmann.adapterdelegates4.AsyncListDifferDelegationAdapter
import org.koitharu.kotatsu.settings.sources.model.SourceConfigItem

class SourceConfigAdapter(
	listener: SourceConfigListener,
) : AsyncListDifferDelegationAdapter<SourceConfigItem>(
	SourceConfigDiffCallback(),
	sourceConfigHeaderDelegate(),
	sourceConfigGroupDelegate(listener),
	sourceConfigItemDelegate(listener),
)