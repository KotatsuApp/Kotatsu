package org.koitharu.kotatsu.reader.ui.pager.wetoon

import android.view.LayoutInflater
import android.view.ViewGroup
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.databinding.ItemPageWebtoonBinding
import org.koitharu.kotatsu.reader.ui.PageLoader
import org.koitharu.kotatsu.reader.ui.pager.BaseReaderAdapter

class WebtoonAdapter(
	loader: PageLoader,
	settings: AppSettings
) : BaseReaderAdapter<WebtoonHolder>(loader, settings) {

	override fun onCreateViewHolder(
		parent: ViewGroup,
		loader: PageLoader,
		settings: AppSettings
	) = WebtoonHolder(
		binding = ItemPageWebtoonBinding.inflate(
			LayoutInflater.from(parent.context),
			parent,
			false
		),
		loader = loader,
		settings = settings
	)
}