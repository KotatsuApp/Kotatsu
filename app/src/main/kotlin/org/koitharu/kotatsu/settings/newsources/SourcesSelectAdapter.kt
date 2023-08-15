package org.koitharu.kotatsu.settings.newsources

import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import org.koitharu.kotatsu.core.ui.BaseListAdapter
import org.koitharu.kotatsu.settings.sources.adapter.SourceConfigListener
import org.koitharu.kotatsu.settings.sources.adapter.sourceConfigItemCheckableDelegate
import org.koitharu.kotatsu.settings.sources.model.SourceConfigItem

class SourcesSelectAdapter(
	listener: SourceConfigListener,
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
) : BaseListAdapter<SourceConfigItem>() {

	init {
		delegatesManager.addDelegate(sourceConfigItemCheckableDelegate(listener, coil, lifecycleOwner))
	}
}
