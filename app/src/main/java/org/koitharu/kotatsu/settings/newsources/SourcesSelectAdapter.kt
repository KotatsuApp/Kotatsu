package org.koitharu.kotatsu.settings.newsources

import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import com.hannesdorfmann.adapterdelegates4.AsyncListDifferDelegationAdapter
import org.koitharu.kotatsu.settings.sources.adapter.SourceConfigDiffCallback
import org.koitharu.kotatsu.settings.sources.adapter.SourceConfigListener
import org.koitharu.kotatsu.settings.sources.adapter.sourceConfigItemCheckableDelegate
import org.koitharu.kotatsu.settings.sources.model.SourceConfigItem

class SourcesSelectAdapter(
	listener: SourceConfigListener,
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
) : AsyncListDifferDelegationAdapter<SourceConfigItem>(
	SourceConfigDiffCallback(),
	sourceConfigItemCheckableDelegate(listener, coil, lifecycleOwner),
)
