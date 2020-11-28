package org.koitharu.kotatsu.settings.sources

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_source_config.*
import org.koitharu.kotatsu.base.domain.MangaProviderFactory
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.utils.ext.mapToSet
import org.koitharu.kotatsu.utils.ext.safe

class SourcesAdapter(
	private val settings: AppSettings,
	private val onItemClickListener: OnListItemClickListener<MangaSource>,
) : RecyclerView.Adapter<SourceViewHolder>() {

	private val dataSet = MangaProviderFactory.getSources(settings, includeHidden = true).toMutableList()
	private val hiddenItems = settings.hiddenSources.mapNotNull {
		safe {
			MangaSource.valueOf(it)
		}
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
		holder.imageView_hidden.setOnCheckedChangeListener {
			if (it) {
				hiddenItems.remove(holder.requireData())
			} else {
				hiddenItems.add(holder.requireData())
			}
			settings.hiddenSources = hiddenItems.mapToSet { x -> x.name }
		}
		holder.imageView_config.setOnClickListener { v ->
			onItemClickListener.onItemClick(holder.requireData(), v)
		}
		holder.imageView_handle.setOnTouchListener { v, event ->
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