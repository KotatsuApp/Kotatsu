package org.koitharu.kotatsu.reader.ui.pager.webtoon

import android.view.LayoutInflater
import android.view.ViewGroup
import org.koitharu.kotatsu.core.exceptions.resolve.ExceptionResolver
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.databinding.ItemPageWebtoonBinding
import org.koitharu.kotatsu.reader.domain.PageLoader
import org.koitharu.kotatsu.reader.ui.config.ReaderSettings
import org.koitharu.kotatsu.reader.ui.pager.BaseReaderAdapter

class WebtoonAdapter(
	loader: PageLoader,
	settings: ReaderSettings,
	exceptionResolver: ExceptionResolver,
) : BaseReaderAdapter<WebtoonHolder>(loader, settings, exceptionResolver) {

	override fun onCreateViewHolder(
		parent: ViewGroup,
		loader: PageLoader,
		settings: ReaderSettings,
		exceptionResolver: ExceptionResolver,
	) = WebtoonHolder(
		binding = ItemPageWebtoonBinding.inflate(
			LayoutInflater.from(parent.context),
			parent,
			false,
		),
		loader = loader,
		settings = settings,
		exceptionResolver = exceptionResolver,
	)
}
