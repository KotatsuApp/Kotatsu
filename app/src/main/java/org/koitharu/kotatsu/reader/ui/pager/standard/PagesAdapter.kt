package org.koitharu.kotatsu.reader.ui.pager.standard

import android.view.LayoutInflater
import android.view.ViewGroup
import org.koitharu.kotatsu.core.exceptions.resolve.ExceptionResolver
import org.koitharu.kotatsu.databinding.ItemPageBinding
import org.koitharu.kotatsu.reader.domain.PageLoader
import org.koitharu.kotatsu.reader.ui.config.ReaderSettings
import org.koitharu.kotatsu.reader.ui.pager.BaseReaderAdapter

class PagesAdapter(
	loader: PageLoader,
	settings: ReaderSettings,
	exceptionResolver: ExceptionResolver,
) : BaseReaderAdapter<PageHolder>(loader, settings, exceptionResolver) {

	override fun onCreateViewHolder(
		parent: ViewGroup,
		loader: PageLoader,
		settings: ReaderSettings,
		exceptionResolver: ExceptionResolver,
	) = PageHolder(
		binding = ItemPageBinding.inflate(LayoutInflater.from(parent.context), parent, false),
		loader = loader,
		settings = settings,
		exceptionResolver = exceptionResolver,
	)
}
