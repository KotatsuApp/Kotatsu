package org.koitharu.kotatsu.ui.settings.sources

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_source_config.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.domain.MangaProviderFactory
import org.koitharu.kotatsu.ui.common.list.OnRecyclerItemClickListener
import org.koitharu.kotatsu.utils.ext.safe

class SourcesAdapter(private val onItemClickListener: OnRecyclerItemClickListener<MangaSource>) :
	RecyclerView.Adapter<SourceViewHolder>(), KoinComponent {

	private val dataSet = MangaProviderFactory.getSources(includeHidden = true).toMutableList()
	private val settings by inject<AppSettings>()
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
			settings.hiddenSources = hiddenItems.map { x -> x.name }.toSet()
		}
		holder.imageView_config.setOnClickListener { v ->
			onItemClickListener.onItemClick(holder.requireData(), holder.bindingAdapterPosition, v)
		}
		holder.imageView_handle.setOnTouchListener { v, event ->
			if (event.actionMasked == MotionEvent.ACTION_DOWN) {
				onItemClickListener.onItemLongClick(
					holder.requireData(),
					holder.bindingAdapterPosition,
					v
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