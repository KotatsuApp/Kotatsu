package org.koitharu.kotatsu.reader.ui.pager.reversed

import android.view.LayoutInflater
import android.view.ViewGroup
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.databinding.ItemPageBinding
import org.koitharu.kotatsu.reader.ui.PageLoader
import org.koitharu.kotatsu.reader.ui.pager.BaseReaderAdapter

class ReversedPagesAdapter(
	loader: PageLoader,
	settings: AppSettings
) : BaseReaderAdapter<ReversedPageHolder>(loader, settings) {

	override fun onCreateViewHolder(
		parent: ViewGroup,
		loader: PageLoader,
		settings: AppSettings
	) = ReversedPageHolder(
		binding = ItemPageBinding.inflate(LayoutInflater.from(parent.context), parent, false),
		loader = loader,
		settings = settings
	)
}