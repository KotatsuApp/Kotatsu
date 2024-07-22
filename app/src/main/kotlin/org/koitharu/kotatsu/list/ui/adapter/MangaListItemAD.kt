package org.koitharu.kotatsu.list.ui.adapter

import android.view.View
import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import com.google.android.material.badge.BadgeDrawable
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.core.ui.image.TrimTransformation
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.util.ext.defaultPlaceholders
import org.koitharu.kotatsu.core.util.ext.enqueueWith
import org.koitharu.kotatsu.core.util.ext.newImageRequest
import org.koitharu.kotatsu.core.util.ext.setOnContextClickListenerCompat
import org.koitharu.kotatsu.core.util.ext.source
import org.koitharu.kotatsu.core.util.ext.textAndVisible
import org.koitharu.kotatsu.databinding.ItemMangaListBinding
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.list.ui.model.MangaCompactListModel
import org.koitharu.kotatsu.parsers.model.Manga

fun mangaListItemAD(
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
	clickListener: OnListItemClickListener<Manga>,
) = adapterDelegateViewBinding<MangaCompactListModel, ListModel, ItemMangaListBinding>(
	{ inflater, parent -> ItemMangaListBinding.inflate(inflater, parent, false) },
) {
	var badge: BadgeDrawable? = null

	val eventListener = object : View.OnClickListener, View.OnLongClickListener {
		override fun onClick(v: View) = clickListener.onItemClick(item.manga, v)
		override fun onLongClick(v: View): Boolean = clickListener.onItemLongClick(item.manga, v)
	}
	itemView.setOnClickListener(eventListener)
	itemView.setOnLongClickListener(eventListener)
	itemView.setOnContextClickListenerCompat(eventListener)

	bind {
		binding.textViewTitle.text = item.title
		binding.textViewSubtitle.textAndVisible = item.subtitle
		binding.imageViewCover.newImageRequest(lifecycleOwner, item.coverUrl)?.run {
			defaultPlaceholders(context)
			allowRgb565(true)
			transformations(TrimTransformation())
			tag(item.manga)
			source(item.source)
			enqueueWith(coil)
		}
		badge = itemView.bindBadge(badge, item.counter)
	}
}
