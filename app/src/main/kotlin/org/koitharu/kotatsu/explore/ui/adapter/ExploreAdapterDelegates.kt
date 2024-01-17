package org.koitharu.kotatsu.explore.ui.adapter

import android.view.View
import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.getSummary
import org.koitharu.kotatsu.core.model.getTitle
import org.koitharu.kotatsu.core.parser.favicon.faviconUri
import org.koitharu.kotatsu.core.ui.image.FaviconDrawable
import org.koitharu.kotatsu.core.ui.image.TrimTransformation
import org.koitharu.kotatsu.core.ui.list.AdapterDelegateClickListenerAdapter
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.util.ext.enqueueWith
import org.koitharu.kotatsu.core.util.ext.newImageRequest
import org.koitharu.kotatsu.core.util.ext.setOnContextClickListenerCompat
import org.koitharu.kotatsu.core.util.ext.setProgressIcon
import org.koitharu.kotatsu.core.util.ext.source
import org.koitharu.kotatsu.core.util.ext.textAndVisible
import org.koitharu.kotatsu.databinding.ItemExploreButtonsBinding
import org.koitharu.kotatsu.databinding.ItemExploreSourceGridBinding
import org.koitharu.kotatsu.databinding.ItemExploreSourceListBinding
import org.koitharu.kotatsu.databinding.ItemRecommendationBinding
import org.koitharu.kotatsu.explore.ui.model.ExploreButtons
import org.koitharu.kotatsu.explore.ui.model.MangaSourceItem
import org.koitharu.kotatsu.explore.ui.model.RecommendationsItem
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.parsers.model.Manga

fun exploreButtonsAD(
	clickListener: View.OnClickListener,
) = adapterDelegateViewBinding<ExploreButtons, ListModel, ItemExploreButtonsBinding>(
	{ layoutInflater, parent -> ItemExploreButtonsBinding.inflate(layoutInflater, parent, false) },
) {

	binding.buttonBookmarks.setOnClickListener(clickListener)
	binding.buttonDownloads.setOnClickListener(clickListener)
	binding.buttonLocal.setOnClickListener(clickListener)
	binding.buttonRandom.setOnClickListener(clickListener)

	bind {
		if (item.isRandomLoading) {
			binding.buttonRandom.setProgressIcon()
		} else {
			binding.buttonRandom.setIconResource(R.drawable.ic_dice)
		}
		binding.buttonRandom.isClickable = !item.isRandomLoading
	}
}

fun exploreRecommendationItemAD(
	coil: ImageLoader,
	clickListener: View.OnClickListener,
	itemClickListener: OnListItemClickListener<Manga>,
	lifecycleOwner: LifecycleOwner,
) = adapterDelegateViewBinding<RecommendationsItem, ListModel, ItemRecommendationBinding>(
	{ layoutInflater, parent -> ItemRecommendationBinding.inflate(layoutInflater, parent, false) },
) {

	binding.buttonMore.setOnClickListener(clickListener)
	binding.root.setOnClickListener { v ->
		itemClickListener.onItemClick(item.manga, v)
	}

	bind {
		binding.textViewTitle.text = item.manga.title
		binding.textViewSubtitle.textAndVisible = item.summary
		binding.imageViewCover.newImageRequest(lifecycleOwner, item.manga.coverUrl)?.run {
			placeholder(R.drawable.ic_placeholder)
			fallback(R.drawable.ic_placeholder)
			error(R.drawable.ic_error_placeholder)
			allowRgb565(true)
			transformations(TrimTransformation())
			source(item.manga.source)
			enqueueWith(coil)
		}
	}
}

fun exploreSourceListItemAD(
	coil: ImageLoader,
	listener: OnListItemClickListener<MangaSourceItem>,
	lifecycleOwner: LifecycleOwner,
) = adapterDelegateViewBinding<MangaSourceItem, ListModel, ItemExploreSourceListBinding>(
	{ layoutInflater, parent ->
		ItemExploreSourceListBinding.inflate(
			layoutInflater,
			parent,
			false,
		)
	},
	on = { item, _, _ -> item is MangaSourceItem && !item.isGrid },
) {

	val eventListener = AdapterDelegateClickListenerAdapter(this, listener)

	binding.root.setOnClickListener(eventListener)
	binding.root.setOnLongClickListener(eventListener)
	binding.root.setOnContextClickListenerCompat(eventListener)

	bind {
		binding.textViewTitle.text = item.source.getTitle(context)
		binding.textViewSubtitle.text = item.source.getSummary(context)
		val fallbackIcon = FaviconDrawable(context, R.style.FaviconDrawable_Small, item.source.name)
		binding.imageViewIcon.newImageRequest(lifecycleOwner, item.source.faviconUri())?.run {
			fallback(fallbackIcon)
			placeholder(fallbackIcon)
			error(fallbackIcon)
			source(item.source)
			enqueueWith(coil)
		}
	}
}

fun exploreSourceGridItemAD(
	coil: ImageLoader,
	listener: OnListItemClickListener<MangaSourceItem>,
	lifecycleOwner: LifecycleOwner,
) = adapterDelegateViewBinding<MangaSourceItem, ListModel, ItemExploreSourceGridBinding>(
	{ layoutInflater, parent ->
		ItemExploreSourceGridBinding.inflate(
			layoutInflater,
			parent,
			false,
		)
	},
	on = { item, _, _ -> item is MangaSourceItem && item.isGrid },
) {

	val eventListener = AdapterDelegateClickListenerAdapter(this, listener)

	binding.root.setOnClickListener(eventListener)
	binding.root.setOnLongClickListener(eventListener)
	binding.root.setOnContextClickListenerCompat(eventListener)

	bind {
		binding.textViewTitle.text = item.source.getTitle(context)
		val fallbackIcon = FaviconDrawable(context, R.style.FaviconDrawable_Large, item.source.name)
		binding.imageViewIcon.newImageRequest(lifecycleOwner, item.source.faviconUri())?.run {
			fallback(fallbackIcon)
			placeholder(fallbackIcon)
			error(fallbackIcon)
			source(item.source)
			enqueueWith(coil)
		}
	}
}
