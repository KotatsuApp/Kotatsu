package org.koitharu.kotatsu.ui.reader.reversed

import android.view.ViewGroup
import org.koitharu.kotatsu.ui.base.list.BaseViewHolder
import org.koitharu.kotatsu.ui.reader.PageLoader
import org.koitharu.kotatsu.ui.reader.base.BaseReaderAdapter
import org.koitharu.kotatsu.ui.reader.base.ReaderPage
import org.koitharu.kotatsu.ui.reader.standard.PageHolder

class ReversedPagesAdapter(
	pages: List<ReaderPage>,
	private val loader: PageLoader
) : BaseReaderAdapter(pages) {

	override fun onCreateViewHolder(parent: ViewGroup) = PageHolder(parent, loader)

	override fun onBindViewHolder(holder: BaseViewHolder<ReaderPage, Unit>, position: Int) {
		super.onBindViewHolder(holder, reversed(position))
	}

	override fun getItem(position: Int): ReaderPage {
		return super.getItem(reversed(position))
	}

	override fun getItemId(position: Int): Long {
		return super.getItemId(reversed(position))
	}

	override fun notifyItemsAppended(count: Int) {
		super.notifyItemsPrepended(count)
	}

	override fun notifyItemsPrepended(count: Int) {
		super.notifyItemsAppended(count)
	}

	override fun notifyItemsRemovedStart(count: Int) {
		super.notifyItemsRemovedEnd(count)
	}

	override fun notifyItemsRemovedEnd(count: Int) {
		super.notifyItemsRemovedStart(count)
	}

	private fun reversed(position: Int) = pages.size - position - 1
}