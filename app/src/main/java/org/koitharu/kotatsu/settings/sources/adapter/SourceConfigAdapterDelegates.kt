package org.koitharu.kotatsu.settings.sources.adapter

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.widget.CompoundButton
import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegate
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.parser.favicon.faviconUri
import org.koitharu.kotatsu.databinding.ItemExpandableBinding
import org.koitharu.kotatsu.databinding.ItemFilterHeaderBinding
import org.koitharu.kotatsu.databinding.ItemSourceConfigBinding
import org.koitharu.kotatsu.databinding.ItemSourceConfigDraggableBinding
import org.koitharu.kotatsu.settings.sources.model.SourceConfigItem
import org.koitharu.kotatsu.utils.ext.*
import org.koitharu.kotatsu.utils.image.FaviconFallbackDrawable

fun sourceConfigHeaderDelegate() =
	adapterDelegateViewBinding<SourceConfigItem.Header, SourceConfigItem, ItemFilterHeaderBinding>(
		{ layoutInflater, parent -> ItemFilterHeaderBinding.inflate(layoutInflater, parent, false) }
	) {

		bind {
			binding.textViewTitle.setText(item.titleResId)
		}
	}

fun sourceConfigGroupDelegate(
	listener: SourceConfigListener,
) = adapterDelegateViewBinding<SourceConfigItem.LocaleGroup, SourceConfigItem, ItemExpandableBinding>(
	{ layoutInflater, parent -> ItemExpandableBinding.inflate(layoutInflater, parent, false) }
) {

	binding.root.setOnClickListener {
		listener.onHeaderClick(item)
	}

	bind {
		binding.root.text = item.title ?: getString(R.string.various_languages)
		binding.root.isChecked = item.isExpanded
	}
}

fun sourceConfigItemDelegate(
	listener: SourceConfigListener,
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
) = adapterDelegateViewBinding<SourceConfigItem.SourceItem, SourceConfigItem, ItemSourceConfigBinding>(
	{ layoutInflater, parent -> ItemSourceConfigBinding.inflate(layoutInflater, parent, false) },
	on = { item, _, _ -> item is SourceConfigItem.SourceItem && !item.isDraggable }
) {

	binding.switchToggle.setOnCheckedChangeListener { _, isChecked ->
		listener.onItemEnabledChanged(item, isChecked)
	}

	bind {
		binding.textViewTitle.text = item.source.title
		binding.switchToggle.isChecked = item.isEnabled
		binding.textViewDescription.textAndVisible = item.summary
		val fallbackIcon = FaviconFallbackDrawable(context, item.source.name)
		binding.imageViewIcon.newImageRequest(item.source.faviconUri())?.run {
			crossfade(context)
			error(fallbackIcon)
			placeholder(fallbackIcon)
			fallback(fallbackIcon)
			lifecycle(lifecycleOwner)
			enqueueWith(coil)
		}
	}

	onViewRecycled {
		binding.imageViewIcon.disposeImageRequest()
	}
}

@SuppressLint("ClickableViewAccessibility")
fun sourceConfigDraggableItemDelegate(
	listener: SourceConfigListener,
) = adapterDelegateViewBinding<SourceConfigItem.SourceItem, SourceConfigItem, ItemSourceConfigDraggableBinding>(
	{ layoutInflater, parent -> ItemSourceConfigDraggableBinding.inflate(layoutInflater, parent, false) },
	on = { item, _, _ -> item is SourceConfigItem.SourceItem && item.isDraggable }
) {

	val eventListener = object :
		View.OnClickListener,
		View.OnTouchListener,
		CompoundButton.OnCheckedChangeListener {
		override fun onClick(v: View?) = listener.onItemSettingsClick(item)

		override fun onTouch(v: View?, event: MotionEvent): Boolean {
			return if (event.actionMasked == MotionEvent.ACTION_DOWN) {
				listener.onDragHandleTouch(this@adapterDelegateViewBinding)
				true
			} else {
				false
			}
		}

		override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
			listener.onItemEnabledChanged(item, isChecked)
		}
	}

	binding.imageViewConfig.setOnClickListener(eventListener)
	binding.switchToggle.setOnCheckedChangeListener(eventListener)
	binding.imageViewHandle.setOnTouchListener(eventListener)

	bind {
		binding.textViewTitle.text = item.source.title
		binding.textViewDescription.text = item.summary ?: getString(R.string.various_languages)
		binding.switchToggle.isChecked = item.isEnabled
	}
}

fun sourceConfigEmptySearchDelegate() = adapterDelegate<SourceConfigItem.EmptySearchResult, SourceConfigItem>(
	R.layout.item_sources_empty
) { }