package org.koitharu.kotatsu.tracker.ui.debug

import android.graphics.Color
import android.text.format.DateUtils
import androidx.core.content.ContextCompat
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.lifecycle.LifecycleOwner
import coil3.ImageLoader
import coil3.request.allowRgb565
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.util.ext.defaultPlaceholders
import org.koitharu.kotatsu.core.util.ext.drawableStart
import org.koitharu.kotatsu.core.util.ext.enqueueWith
import org.koitharu.kotatsu.core.util.ext.getThemeColor
import org.koitharu.kotatsu.core.util.ext.mangaSourceExtra
import org.koitharu.kotatsu.core.util.ext.newImageRequest
import org.koitharu.kotatsu.databinding.ItemTrackDebugBinding
import org.koitharu.kotatsu.tracker.data.TrackEntity
import com.google.android.material.R as materialR

fun trackDebugAD(
	lifecycleOwner: LifecycleOwner,
	coil: ImageLoader,
	clickListener: OnListItemClickListener<TrackDebugItem>,
) = adapterDelegateViewBinding<TrackDebugItem, TrackDebugItem, ItemTrackDebugBinding>(
	{ layoutInflater, parent -> ItemTrackDebugBinding.inflate(layoutInflater, parent, false) },
) {
	val indicatorNew = ContextCompat.getDrawable(context, R.drawable.ic_new)

	itemView.setOnClickListener { v ->
		clickListener.onItemClick(item, v)
	}

	bind {
		binding.imageViewCover.newImageRequest(lifecycleOwner, item.manga.coverUrl)?.run {
			defaultPlaceholders(context)
			allowRgb565(true)
			mangaSourceExtra(item.manga.source)
			enqueueWith(coil)
		}
		binding.textViewTitle.text = item.manga.title
		binding.textViewSummary.text = buildSpannedString {
			append(
				item.lastCheckTime?.let {
					DateUtils.getRelativeDateTimeString(
						context,
						it.toEpochMilli(),
						DateUtils.MINUTE_IN_MILLIS,
						DateUtils.WEEK_IN_MILLIS,
						0,
					)
				} ?: getString(R.string.never),
			)
			if (item.lastResult == TrackEntity.RESULT_FAILED) {
				append(" - ")
				bold {
					color(context.getThemeColor(materialR.attr.colorError, Color.RED)) {
						append(item.lastError ?: getString(R.string.error))
					}
				}
			}
		}
		binding.textViewTitle.drawableStart = if (item.newChapters > 0) {
			indicatorNew
		} else {
			null
		}
	}
}
