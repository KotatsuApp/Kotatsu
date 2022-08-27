package org.koitharu.kotatsu.reader.ui.pager

import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import org.koitharu.kotatsu.core.exceptions.resolve.ExceptionResolver
import org.koitharu.kotatsu.reader.domain.PageLoader
import org.koitharu.kotatsu.reader.ui.config.ReaderSettings
import org.koitharu.kotatsu.utils.ext.resetTransformations

@Suppress("LeakingThis")
abstract class BaseReaderAdapter<H : BasePageHolder<*>>(
	private val loader: PageLoader,
	private val readerSettings: ReaderSettings,
	private val exceptionResolver: ExceptionResolver,
) : RecyclerView.Adapter<H>() {

	private val differ = AsyncListDiffer(this, DiffCallback())

	init {
		setHasStableIds(true)
		stateRestorationPolicy = StateRestorationPolicy.PREVENT
	}

	override fun onBindViewHolder(holder: H, position: Int) {
		holder.bind(differ.currentList[position])
	}

	override fun onViewRecycled(holder: H) {
		holder.onRecycled()
		holder.itemView.resetTransformations()
		super.onViewRecycled(holder)
	}

	override fun onViewAttachedToWindow(holder: H) {
		super.onViewAttachedToWindow(holder)
		holder.onAttachedToWindow()
	}

	override fun onViewDetachedFromWindow(holder: H) {
		holder.onDetachedFromWindow()
		super.onViewDetachedFromWindow(holder)
	}

	open fun getItem(position: Int): ReaderPage = differ.currentList[position]

	open fun getItemOrNull(position: Int) = differ.currentList.getOrNull(position)

	override fun getItemId(position: Int) = differ.currentList[position].id

	final override fun getItemCount() = differ.currentList.size

	final override fun onCreateViewHolder(
		parent: ViewGroup,
		viewType: Int,
	): H = onCreateViewHolder(parent, loader, readerSettings, exceptionResolver)

	suspend fun setItems(items: List<ReaderPage>) = suspendCoroutine<Unit> { cont ->
		differ.submitList(items) {
			cont.resume(Unit)
		}
	}

	protected abstract fun onCreateViewHolder(
		parent: ViewGroup,
		loader: PageLoader,
		settings: ReaderSettings,
		exceptionResolver: ExceptionResolver,
	): H

	private class DiffCallback : DiffUtil.ItemCallback<ReaderPage>() {

		override fun areItemsTheSame(oldItem: ReaderPage, newItem: ReaderPage): Boolean {
			return oldItem.id == newItem.id
		}

		override fun areContentsTheSame(oldItem: ReaderPage, newItem: ReaderPage): Boolean {
			return oldItem == newItem
		}
	}
}
