package org.koitharu.kotatsu.reader.ui.pager

import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import org.koitharu.kotatsu.core.exceptions.resolve.ExceptionResolver
import org.koitharu.kotatsu.core.os.NetworkState
import org.koitharu.kotatsu.core.util.ext.resetTransformations
import org.koitharu.kotatsu.reader.domain.PageLoader
import org.koitharu.kotatsu.reader.ui.config.ReaderSettings
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Suppress("LeakingThis")
abstract class BaseReaderAdapter<H : BasePageHolder<*>>(
	private val loader: PageLoader,
	private val readerSettings: ReaderSettings,
	private val networkState: NetworkState,
	private val exceptionResolver: ExceptionResolver,
) : RecyclerView.Adapter<H>() {

	private val differ = AsyncListDiffer(this, DiffCallback())

	val hasItems: Boolean
		get() = itemCount != 0

	init {
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

	final override fun getItemCount() = differ.currentList.size

	final override fun onCreateViewHolder(
		parent: ViewGroup,
		viewType: Int,
	): H = onCreateViewHolder(parent, loader, readerSettings, networkState, exceptionResolver)

	suspend fun setItems(items: List<ReaderPage>) = suspendCoroutine { cont ->
		differ.submitList(items) {
			cont.resume(Unit)
		}
	}

	protected abstract fun onCreateViewHolder(
		parent: ViewGroup,
		loader: PageLoader,
		settings: ReaderSettings,
		networkState: NetworkState,
		exceptionResolver: ExceptionResolver,
	): H

	private class DiffCallback : DiffUtil.ItemCallback<ReaderPage>() {

		override fun areItemsTheSame(oldItem: ReaderPage, newItem: ReaderPage): Boolean {
			return oldItem.id == newItem.id && oldItem.chapterId == newItem.chapterId
		}

		override fun areContentsTheSame(oldItem: ReaderPage, newItem: ReaderPage): Boolean {
			return oldItem == newItem
		}
	}
}
