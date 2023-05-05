package org.koitharu.kotatsu.download.ui.list

import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.work.WorkInfo
import coil.ImageLoader
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.databinding.ItemDownloadBinding
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.parsers.util.format
import org.koitharu.kotatsu.utils.ext.disposeImageRequest
import org.koitharu.kotatsu.utils.ext.enqueueWith
import org.koitharu.kotatsu.utils.ext.getDisplayMessage
import org.koitharu.kotatsu.utils.ext.newImageRequest
import org.koitharu.kotatsu.utils.ext.source

fun downloadItemAD(
	lifecycleOwner: LifecycleOwner,
	coil: ImageLoader,
	listener: DownloadItemListener,
) = adapterDelegateViewBinding<DownloadItemModel, ListModel, ItemDownloadBinding>(
	{ inflater, parent -> ItemDownloadBinding.inflate(inflater, parent, false) },
) {

	val percentPattern = context.resources.getString(R.string.percent_string_pattern)

	val clickListener = View.OnClickListener { v ->
		when (v.id) {
			R.id.button_cancel -> listener.onCancelClick(item)
			R.id.button_resume -> listener.onResumeClick(item)
			R.id.button_pause -> listener.onPauseClick(item)
			else -> listener.onItemClick(item, v)
		}
	}
	binding.buttonCancel.setOnClickListener(clickListener)
	binding.buttonPause.setOnClickListener(clickListener)
	binding.buttonResume.setOnClickListener(clickListener)
	itemView.setOnClickListener(clickListener)

	bind { payloads ->
		binding.textViewTitle.text = item.manga.title
		binding.imageViewCover.newImageRequest(lifecycleOwner, item.manga.coverUrl)?.apply {
			placeholder(R.drawable.ic_placeholder)
			fallback(R.drawable.ic_placeholder)
			error(R.drawable.ic_error_placeholder)
			allowRgb565(true)
			source(item.manga.source)
			enqueueWith(coil)
		}
		when (item.workState) {
			WorkInfo.State.ENQUEUED,
			WorkInfo.State.BLOCKED -> {
				binding.textViewStatus.setText(R.string.queued)
				binding.progressBar.isIndeterminate = false
				binding.progressBar.isVisible = false
				binding.textViewPercent.isVisible = false
				binding.textViewDetails.isVisible = false
				binding.buttonCancel.isVisible = true
				binding.buttonResume.isVisible = false
			}

			WorkInfo.State.RUNNING -> {
				binding.textViewStatus.setText(R.string.manga_downloading_)
				binding.progressBar.isIndeterminate = false
				binding.progressBar.isVisible = true
				binding.progressBar.max = item.max
				binding.progressBar.setProgressCompat(item.progress, payloads.isNotEmpty())
				binding.textViewPercent.text = percentPattern.format((item.percent * 100f).format(1))
				binding.textViewPercent.isVisible = true
				binding.textViewDetails.isVisible = false
				binding.buttonCancel.isVisible = true
				binding.buttonResume.isVisible = false
			}

			WorkInfo.State.SUCCEEDED -> {
				binding.textViewStatus.setText(R.string.download_complete)
				binding.progressBar.isIndeterminate = false
				binding.progressBar.isVisible = false
				binding.textViewPercent.isVisible = false
				binding.textViewDetails.isVisible = false
				binding.buttonCancel.isVisible = false
				binding.buttonResume.isVisible = false
			}

			WorkInfo.State.FAILED -> {
				binding.textViewStatus.setText(R.string.error_occurred)
				binding.progressBar.isIndeterminate = false
				binding.progressBar.isVisible = false
				binding.textViewPercent.isVisible = false
				binding.textViewDetails.text = item.error?.getDisplayMessage(context.resources)
				binding.textViewDetails.isVisible = true
				binding.buttonCancel.isVisible = false
				binding.buttonResume.isVisible = true
			}

			WorkInfo.State.CANCELLED -> {
				binding.textViewStatus.setText(R.string.canceled)
				binding.progressBar.isIndeterminate = false
				binding.progressBar.isVisible = false
				binding.textViewPercent.isVisible = false
				binding.textViewDetails.isVisible = false
				binding.buttonCancel.isVisible = false
				binding.buttonResume.isVisible = false
			}
		}
	}

	onViewRecycled {
		binding.imageViewCover.disposeImageRequest()
	}
}
