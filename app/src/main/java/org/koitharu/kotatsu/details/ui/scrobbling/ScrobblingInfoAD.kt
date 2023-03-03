package org.koitharu.kotatsu.details.ui.scrobbling

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.databinding.ItemScrobblingInfoBinding
import org.koitharu.kotatsu.scrobbling.common.domain.model.ScrobblingInfo
import org.koitharu.kotatsu.utils.ext.disposeImageRequest
import org.koitharu.kotatsu.utils.ext.enqueueWith
import org.koitharu.kotatsu.utils.ext.newImageRequest

fun scrobblingInfoAD(
	lifecycleOwner: LifecycleOwner,
	coil: ImageLoader,
	fragmentManager: FragmentManager,
) = adapterDelegateViewBinding<ScrobblingInfo, ScrobblingInfo, ItemScrobblingInfoBinding>(
	{ layoutInflater, parent -> ItemScrobblingInfoBinding.inflate(layoutInflater, parent, false) },
) {
	binding.root.setOnClickListener {
		ScrobblingInfoBottomSheet.show(fragmentManager, bindingAdapterPosition)
	}

	bind {
		binding.imageViewCover.newImageRequest(lifecycleOwner, item.coverUrl)?.run {
			placeholder(R.drawable.ic_placeholder)
			fallback(R.drawable.ic_placeholder)
			error(R.drawable.ic_error_placeholder)
			enqueueWith(coil)
		}
		binding.textViewTitle.text = item.title
		binding.textViewTitle.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, item.scrobbler.iconResId, 0)
		binding.ratingBar.rating = item.rating * binding.ratingBar.numStars
		binding.textViewStatus.text = item.status?.let {
			context.resources.getStringArray(R.array.scrobbling_statuses).getOrNull(it.ordinal)
		}
	}

	onViewRecycled {
		binding.imageViewCover.disposeImageRequest()
	}
}
