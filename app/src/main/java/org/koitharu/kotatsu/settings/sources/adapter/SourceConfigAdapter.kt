package org.koitharu.kotatsu.settings.sources.adapter

import com.hannesdorfmann.adapterdelegates4.AsyncListDifferDelegationAdapter

class SourceConfigAdapter(
	listener: SourceConfigListener,
) : AsyncListDifferDelegationAdapter<SourceConfigItem>(
	SourceConfigDiffCallback(),
	sourceConfigHeaderDelegate(listener),
	sourceConfigItemDelegate(listener),
)