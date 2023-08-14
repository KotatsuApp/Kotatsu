package org.koitharu.kotatsu.settings.sources.adapter

import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import org.koitharu.kotatsu.core.ui.BaseListAdapter
import org.koitharu.kotatsu.settings.sources.model.SourceConfigItem

class SourceConfigAdapter(
	listener: SourceConfigListener,
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
) : BaseListAdapter<SourceConfigItem>(
	sourceConfigHeaderDelegate(),
	sourceConfigGroupDelegate(listener),
	sourceConfigItemDelegate2(listener, coil, lifecycleOwner),
	sourceConfigEmptySearchDelegate(),
	sourceConfigTipDelegate(listener),
)
