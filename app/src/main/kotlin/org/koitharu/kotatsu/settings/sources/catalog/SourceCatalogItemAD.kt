package org.koitharu.kotatsu.settings.sources.catalog

import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.getSummary
import org.koitharu.kotatsu.core.model.getTitle
import org.koitharu.kotatsu.core.parser.favicon.faviconUri
import org.koitharu.kotatsu.core.ui.image.FaviconDrawable
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.util.ext.crossfade
import org.koitharu.kotatsu.core.util.ext.enqueueWith
import org.koitharu.kotatsu.core.util.ext.newImageRequest
import org.koitharu.kotatsu.core.util.ext.setTextAndVisible
import org.koitharu.kotatsu.core.util.ext.source
import org.koitharu.kotatsu.databinding.ItemEmptyHintBinding
import org.koitharu.kotatsu.databinding.ItemSourceCatalogBinding

fun sourceCatalogItemSourceAD(
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
	listener: OnListItemClickListener<SourceCatalogItem.Source>
) = adapterDelegateViewBinding<SourceCatalogItem.Source, SourceCatalogItem, ItemSourceCatalogBinding>(
	{ layoutInflater, parent ->
		ItemSourceCatalogBinding.inflate(layoutInflater, parent, false)
	},
) {

	binding.imageViewAdd.setOnClickListener { v ->
		listener.onItemClick(item, v)
	}

	bind {
		binding.textViewTitle.text = item.source.getTitle(context)
		if (item.showSummary) {
			binding.textViewDescription.text = item.source.getSummary(context)
			binding.textViewDescription.isVisible = true
		} else {
			binding.textViewDescription.isVisible = false
		}
		val fallbackIcon = FaviconDrawable(context, R.style.FaviconDrawable_Small, item.source.name)
		binding.imageViewIcon.newImageRequest(lifecycleOwner, item.source.faviconUri())?.run {
			crossfade(context)
			error(fallbackIcon)
			placeholder(fallbackIcon)
			fallback(fallbackIcon)
			source(item.source)
			enqueueWith(coil)
		}
	}
}

fun sourceCatalogItemHintAD(
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
) = adapterDelegateViewBinding<SourceCatalogItem.Hint, SourceCatalogItem, ItemEmptyHintBinding>(
	{ inflater, parent -> ItemEmptyHintBinding.inflate(inflater, parent, false) },
) {

	binding.buttonRetry.isVisible = false

	bind {
		binding.icon.newImageRequest(lifecycleOwner, item.icon)?.enqueueWith(coil)
		binding.textPrimary.setText(item.title)
		binding.textSecondary.setTextAndVisible(item.text)
	}
}
