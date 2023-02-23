package org.koitharu.kotatsu.settings.sources.adapter

import android.view.View
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegate
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.list.OnTipCloseListener
import org.koitharu.kotatsu.core.parser.favicon.faviconUri
import org.koitharu.kotatsu.databinding.ItemExpandableBinding
import org.koitharu.kotatsu.databinding.ItemFilterHeaderBinding
import org.koitharu.kotatsu.databinding.ItemSourceConfigBinding
import org.koitharu.kotatsu.databinding.ItemSourceConfigCheckableBinding
import org.koitharu.kotatsu.databinding.ItemTipBinding
import org.koitharu.kotatsu.settings.sources.model.SourceConfigItem
import org.koitharu.kotatsu.utils.ext.crossfade
import org.koitharu.kotatsu.utils.ext.disposeImageRequest
import org.koitharu.kotatsu.utils.ext.enqueueWith
import org.koitharu.kotatsu.utils.ext.newImageRequest
import org.koitharu.kotatsu.utils.ext.textAndVisible
import org.koitharu.kotatsu.utils.image.FaviconFallbackDrawable

fun sourceConfigHeaderDelegate() =
	adapterDelegateViewBinding<SourceConfigItem.Header, SourceConfigItem, ItemFilterHeaderBinding>(
		{ layoutInflater, parent -> ItemFilterHeaderBinding.inflate(layoutInflater, parent, false) },
	) {

		bind {
			binding.textViewTitle.setText(item.titleResId)
		}
	}

fun sourceConfigGroupDelegate(
	listener: SourceConfigListener,
) = adapterDelegateViewBinding<SourceConfigItem.LocaleGroup, SourceConfigItem, ItemExpandableBinding>(
	{ layoutInflater, parent -> ItemExpandableBinding.inflate(layoutInflater, parent, false) },
) {

	binding.root.setOnClickListener {
		listener.onHeaderClick(item)
	}

	bind {
		binding.root.text = item.title ?: getString(R.string.various_languages)
		binding.root.isChecked = item.isExpanded
	}
}

fun sourceConfigItemCheckableDelegate(
	listener: SourceConfigListener,
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
) = adapterDelegateViewBinding<SourceConfigItem.SourceItem, SourceConfigItem, ItemSourceConfigCheckableBinding>(
	{ layoutInflater, parent -> ItemSourceConfigCheckableBinding.inflate(layoutInflater, parent, false) },
) {

	binding.switchToggle.setOnCheckedChangeListener { _, isChecked ->
		listener.onItemEnabledChanged(item, isChecked)
	}

	bind {
		binding.textViewTitle.text = item.source.title
		binding.switchToggle.isChecked = item.isEnabled
		binding.textViewDescription.textAndVisible = item.summary
		val fallbackIcon = FaviconFallbackDrawable(context, item.source.name)
		binding.imageViewIcon.newImageRequest(item.source.faviconUri(), item.source)?.run {
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

fun sourceConfigItemDelegate2(
	listener: SourceConfigListener,
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
) = adapterDelegateViewBinding<SourceConfigItem.SourceItem, SourceConfigItem, ItemSourceConfigBinding>(
	{ layoutInflater, parent -> ItemSourceConfigBinding.inflate(layoutInflater, parent, false) },
) {

	val eventListener = View.OnClickListener { v ->
		when (v.id) {
			R.id.imageView_add -> listener.onItemEnabledChanged(item, true)
			R.id.imageView_remove -> listener.onItemEnabledChanged(item, false)
			R.id.imageView_config -> listener.onItemSettingsClick(item)
		}
	}
	binding.imageViewRemove.setOnClickListener(eventListener)
	binding.imageViewAdd.setOnClickListener(eventListener)
	binding.imageViewConfig.setOnClickListener(eventListener)

	bind {
		binding.textViewTitle.text = item.source.title
		binding.imageViewAdd.isGone = item.isEnabled
		binding.imageViewRemove.isVisible = item.isEnabled
		binding.imageViewConfig.isVisible = item.isEnabled
		binding.textViewDescription.textAndVisible = item.summary
		val fallbackIcon = FaviconFallbackDrawable(context, item.source.name)
		binding.imageViewIcon.newImageRequest(item.source.faviconUri(), item.source)?.run {
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

fun sourceConfigTipDelegate(
	listener: OnTipCloseListener<SourceConfigItem.Tip>
) = adapterDelegateViewBinding<SourceConfigItem.Tip, SourceConfigItem, ItemTipBinding>(
	{ layoutInflater, parent -> ItemTipBinding.inflate(layoutInflater, parent, false) },
) {

	binding.buttonClose.setOnClickListener {
		listener.onCloseTip(item)
	}

	bind {
		binding.imageViewIcon.setImageResource(item.iconResId)
		binding.textView.setText(item.textResId)
	}
}

fun sourceConfigEmptySearchDelegate() = adapterDelegate<SourceConfigItem.EmptySearchResult, SourceConfigItem>(
	R.layout.item_sources_empty,
) { }
