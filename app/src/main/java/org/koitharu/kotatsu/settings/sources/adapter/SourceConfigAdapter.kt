package org.koitharu.kotatsu.settings.sources.adapter

import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import com.hannesdorfmann.adapterdelegates4.AsyncListDifferDelegationAdapter
import org.koitharu.kotatsu.settings.sources.model.SourceConfigItem

class SourceConfigAdapter(
	listener: SourceConfigListener,
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
) : AsyncListDifferDelegationAdapter<SourceConfigItem>(
	SourceConfigDiffCallback(),
	sourceConfigHeaderDelegate(),
	sourceConfigGroupDelegate(listener),
	sourceConfigItemDelegate2(listener, coil, lifecycleOwner),
	sourceConfigEmptySearchDelegate(),
	sourceConfigTipDelegate(listener),
)
