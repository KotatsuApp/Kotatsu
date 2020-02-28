package org.koitharu.kotatsu.ui.settings.sources

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_source_config.*
import org.koin.core.KoinComponent
import org.koin.core.get
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.domain.MangaProviderFactory
import org.koitharu.kotatsu.ui.common.list.BaseViewHolder
import org.koitharu.kotatsu.ui.common.list.OnRecyclerItemClickListener

class SourcesAdapter(private val onItemClickListener: OnRecyclerItemClickListener<MangaSource>) :
	RecyclerView.Adapter<BaseViewHolder<*, Unit>>(), KoinComponent {

	private val dataSet = MangaProviderFactory.sources.toMutableList()

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = when (viewType) {
		ITEM_SOURCE -> SourceViewHolder(parent).also(::onViewHolderCreated)
		ITEM_DIVIDER -> SourceDividerHolder(parent)
		else -> throw IllegalArgumentException("Unsupported viewType $viewType")
	}

	override fun getItemCount() = dataSet.size

	override fun onBindViewHolder(holder: BaseViewHolder<*, Unit>, position: Int) {
		(holder as? SourceViewHolder)?.bind(dataSet[position], Unit)
	}

	@SuppressLint("ClickableViewAccessibility")
	private fun onViewHolderCreated(holder: SourceViewHolder) {
		holder.imageView_config.setOnClickListener { v ->
			onItemClickListener.onItemClick(holder.requireData(), holder.adapterPosition, v)
		}
		holder.imageView_handle.setOnTouchListener { v, event ->
			if (event.actionMasked == MotionEvent.ACTION_DOWN) {
				onItemClickListener.onItemLongClick(holder.requireData(), holder.adapterPosition, v)
			} else {
				false
			}
		}
	}

	fun moveItem(oldPos: Int, newPos: Int) {
		val item = dataSet.removeAt(oldPos)
		dataSet.add(newPos, item)
		notifyItemMoved(oldPos, newPos)
		get<AppSettings>().sourcesOrder = dataSet.map { it.ordinal }
	}

	companion object {

		const val ITEM_SOURCE = 0
		const val ITEM_DIVIDER = 1
	}
}