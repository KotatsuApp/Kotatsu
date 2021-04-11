package org.koitharu.kotatsu.settings.sources

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.koitharu.kotatsu.base.domain.MangaProviderFactory
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.utils.ext.mapToSet

class SourcesAdapter(
	private val settings: AppSettings,
	private val onItemClickListener: OnListItemClickListener<MangaSource>,
) : RecyclerView.Adapter<SourceViewHolder>() {

	private val dataSet =
		MangaProviderFactory.getSources(settings, includeHidden = true).toMutableList()
	private val hiddenItems = settings.hiddenSources.mapNotNull {
		runCatching {
			MangaSource.valueOf(it)
		}.getOrNull()
	}.toMutableSet()

	override fun onCreateViewHolder(
		parent: ViewGroup,
		viewType: Int
	) = SourceViewHolder(parent).also(::onViewHolderCreated)

	override fun getItemCount() = dataSet.size

	override fun onBindViewHolder(holder: SourceViewHolder, position: Int) {
		val item = dataSet[position]
		holder.bind(item, !hiddenItems.contains(item))
	}

	@SuppressLint("ClickableViewAccessibility")
	private fun onViewHolderCreated(holder: SourceViewHolder) {
		holder.binding.switchToggle.setOnCheckedChangeListener { _, it ->
			if (it) {
				hiddenItems.remove(holder.requireData())
			} else {
				hiddenItems.add(holder.requireData())
			}
			settings.hiddenSources = hiddenItems.mapToSet { x -> x.name }
		}
		holder.binding.imageViewConfig.setOnClickListener { v ->
			onItemClickListener.onItemClick(holder.requireData(), v)
		}
		holder.binding.imageViewHandle.setOnTouchListener { v, event ->
			if (event.actionMasked == MotionEvent.ACTION_DOWN) {
				onItemClickListener.onItemLongClick(
					holder.requireData(),
					holder.itemView
				)
			} else {
				false
			}
		}
	}

	fun moveItem(oldPos: Int, newPos: Int) {
		val item = dataSet.removeAt(oldPos)
		dataSet.add(newPos, item)
		notifyItemMoved(oldPos, newPos)
		settings.sourcesOrder = dataSet.map { it.ordinal }
	}
}