package org.koitharu.kotatsu.details.ui.scrobbling

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import coil3.ImageLoader
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.util.ext.defaultPlaceholders
import org.koitharu.kotatsu.core.util.ext.enqueueWith
import org.koitharu.kotatsu.core.util.ext.newImageRequest
import org.koitharu.kotatsu.databinding.ItemScrobblingInfoBinding
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.scrobbling.common.domain.model.ScrobblingInfo

fun scrobblingInfoAD(
	lifecycleOwner: LifecycleOwner,
	coil: ImageLoader,
	fragmentManager: FragmentManager,
) = adapterDelegateViewBinding<ScrobblingInfo, ListModel, ItemScrobblingInfoBinding>(
	{ layoutInflater, parent -> ItemScrobblingInfoBinding.inflate(layoutInflater, parent, false) },
) {
	binding.root.setOnClickListener {
		ScrobblingInfoSheet.show(fragmentManager, bindingAdapterPosition)
	}

	bind {
		binding.imageViewCover.newImageRequest(lifecycleOwner, item.coverUrl)?.run {
			defaultPlaceholders(context)
			enqueueWith(coil)
		}
		binding.textViewTitle.setText(item.scrobbler.titleResId)
		binding.imageViewIcon.setImageResource(item.scrobbler.iconResId)
		binding.ratingBar.rating = item.rating * binding.ratingBar.numStars
		binding.textViewStatus.text = item.status?.let {
			context.resources.getStringArray(R.array.scrobbling_statuses).getOrNull(it.ordinal)
		}
	}
}
