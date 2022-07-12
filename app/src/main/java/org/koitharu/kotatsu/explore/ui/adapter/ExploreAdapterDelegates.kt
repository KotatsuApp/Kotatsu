package org.koitharu.kotatsu.explore.ui.adapter

import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import coil.request.Disposable
import coil.request.ImageRequest
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegate
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.list.AdapterDelegateClickListenerAdapter
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.parser.favicon.faviconUri
import org.koitharu.kotatsu.databinding.ItemEmptyCardBinding
import org.koitharu.kotatsu.databinding.ItemExploreButtonsBinding
import org.koitharu.kotatsu.databinding.ItemExploreHeaderBinding
import org.koitharu.kotatsu.databinding.ItemExploreSourceBinding
import org.koitharu.kotatsu.explore.ui.model.ExploreItem
import org.koitharu.kotatsu.list.ui.adapter.ListStateHolderListener
import org.koitharu.kotatsu.utils.ext.crossfade
import org.koitharu.kotatsu.utils.ext.enqueueWith
import org.koitharu.kotatsu.utils.ext.setTextAndVisible
import org.koitharu.kotatsu.utils.image.FaviconFallbackDrawable

fun exploreButtonsAD(
	clickListener: View.OnClickListener,
) = adapterDelegateViewBinding<ExploreItem.Buttons, ExploreItem, ItemExploreButtonsBinding>(
	{ layoutInflater, parent -> ItemExploreButtonsBinding.inflate(layoutInflater, parent, false) }
) {

	binding.buttonBookmarks.setOnClickListener(clickListener)
	binding.buttonHistory.setOnClickListener(clickListener)
	binding.buttonLocal.setOnClickListener(clickListener)
	binding.buttonSuggestions.setOnClickListener(clickListener)
	binding.buttonFavourites.setOnClickListener(clickListener)
	binding.buttonRandom.setOnClickListener(clickListener)

	bind {
		binding.buttonSuggestions.isVisible = item.isSuggestionsEnabled
	}
}

fun exploreSourcesHeaderAD(
	listener: ExploreListEventListener,
) = adapterDelegateViewBinding<ExploreItem.Header, ExploreItem, ItemExploreHeaderBinding>(
	{ layoutInflater, parent -> ItemExploreHeaderBinding.inflate(layoutInflater, parent, false) }
) {

	val listenerAdapter = View.OnClickListener {
		listener.onManageClick(itemView)
	}

	binding.buttonMore.setOnClickListener(listenerAdapter)

	bind {
		binding.textViewTitle.setText(item.titleResId)
		binding.buttonMore.isVisible = item.isButtonVisible
	}
}

fun exploreSourceItemAD(
	coil: ImageLoader,
	listener: OnListItemClickListener<ExploreItem.Source>,
	lifecycleOwner: LifecycleOwner,
) = adapterDelegateViewBinding<ExploreItem.Source, ExploreItem, ItemExploreSourceBinding>(
	{ layoutInflater, parent -> ItemExploreSourceBinding.inflate(layoutInflater, parent, false) },
	on = { item, _, _ -> item is ExploreItem.Source }
) {

	var imageRequest: Disposable? = null
	val eventListener = AdapterDelegateClickListenerAdapter(this, listener)

	binding.root.setOnClickListener(eventListener)
	binding.root.setOnLongClickListener(eventListener)

	bind {
		binding.textViewTitle.text = item.source.title
		val fallbackIcon = FaviconFallbackDrawable(context, item.source.name)
		imageRequest = ImageRequest.Builder(context)
			.data(item.source.faviconUri())
			.target(binding.imageViewIcon)
			.crossfade(context)
			.fallback(fallbackIcon)
			.placeholder(fallbackIcon)
			.error(fallbackIcon)
			.lifecycle(lifecycleOwner)
			.enqueueWith(coil)
	}

	onViewRecycled {
		imageRequest?.dispose()
		imageRequest = null
	}
}

fun exploreEmptyHintListAD(
	listener: ListStateHolderListener,
) = adapterDelegateViewBinding<ExploreItem.EmptyHint, ExploreItem, ItemEmptyCardBinding>(
	{ inflater, parent -> ItemEmptyCardBinding.inflate(inflater, parent, false) }
) {

	binding.buttonRetry.setOnClickListener { listener.onEmptyActionClick() }

	bind {
		binding.icon.setImageResource(item.icon)
		binding.textPrimary.setText(item.textPrimary)
		binding.textSecondary.setTextAndVisible(item.textSecondary)
		binding.buttonRetry.setTextAndVisible(item.actionStringRes)
	}
}

fun exploreLoadingAD() = adapterDelegate<ExploreItem.Loading, ExploreItem>(R.layout.item_loading_state) {}