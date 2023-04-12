package org.koitharu.kotatsu.download.ui

import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import coil.ImageLoader
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.databinding.ItemDownloadBinding
import org.koitharu.kotatsu.details.ui.DetailsActivity
import org.koitharu.kotatsu.download.domain.DownloadState
import org.koitharu.kotatsu.parsers.util.format
import org.koitharu.kotatsu.utils.ext.enqueueWith
import org.koitharu.kotatsu.utils.ext.getDisplayMessage
import org.koitharu.kotatsu.utils.ext.newImageRequest
import org.koitharu.kotatsu.utils.ext.onFirst
import org.koitharu.kotatsu.utils.ext.source

fun downloadItemAD(
	lifecycleOwner: LifecycleOwner,
	coil: ImageLoader,
) = adapterDelegateViewBinding<DownloadItem, DownloadItem, ItemDownloadBinding>(
	{ inflater, parent -> ItemDownloadBinding.inflate(inflater, parent, false) },
) {
	var job: Job? = null
	val percentPattern = context.resources.getString(R.string.percent_string_pattern)

	val clickListener = View.OnClickListener { v ->
		when (v.id) {
			R.id.button_cancel -> item.cancel()
			R.id.button_resume -> item.resume()
			else -> context.startActivity(
				DetailsActivity.newIntent(context, item.progressValue.manga),
			)
		}
	}
	binding.buttonCancel.setOnClickListener(clickListener)
	binding.buttonResume.setOnClickListener(clickListener)
	itemView.setOnClickListener(clickListener)

	bind {
		job?.cancel()
		job = item.progressAsFlow().onFirst { state ->
			binding.imageViewCover.newImageRequest(lifecycleOwner, state.manga.coverUrl)?.run {
				placeholder(state.cover)
				fallback(R.drawable.ic_placeholder)
				error(R.drawable.ic_error_placeholder)
				source(state.manga.source)
				allowRgb565(true)
				enqueueWith(coil)
			}
		}.onEach { state ->
			binding.textViewTitle.text = state.manga.title
			when (state) {
				is DownloadState.Cancelled -> {
					binding.textViewStatus.setText(R.string.cancelling_)
					binding.progressBar.isIndeterminate = true
					binding.progressBar.isVisible = true
					binding.textViewPercent.isVisible = false
					binding.textViewDetails.isVisible = false
					binding.buttonCancel.isVisible = false
					binding.buttonResume.isVisible = false
				}

				is DownloadState.Done -> {
					binding.textViewStatus.setText(R.string.download_complete)
					binding.progressBar.isIndeterminate = false
					binding.progressBar.isVisible = false
					binding.textViewPercent.isVisible = false
					binding.textViewDetails.isVisible = false
					binding.buttonCancel.isVisible = false
					binding.buttonResume.isVisible = false
				}

				is DownloadState.Error -> {
					binding.textViewStatus.setText(R.string.error_occurred)
					binding.progressBar.isIndeterminate = false
					binding.progressBar.isVisible = false
					binding.textViewPercent.isVisible = false
					binding.textViewDetails.text = state.error.getDisplayMessage(context.resources)
					binding.textViewDetails.isVisible = true
					binding.buttonCancel.isVisible = state.canRetry
					binding.buttonResume.isVisible = state.canRetry
				}

				is DownloadState.PostProcessing -> {
					binding.textViewStatus.setText(R.string.processing_)
					binding.progressBar.isIndeterminate = true
					binding.progressBar.isVisible = true
					binding.textViewPercent.isVisible = false
					binding.textViewDetails.isVisible = false
					binding.buttonCancel.isVisible = false
					binding.buttonResume.isVisible = false
				}

				is DownloadState.Preparing -> {
					binding.textViewStatus.setText(R.string.preparing_)
					binding.progressBar.isIndeterminate = true
					binding.progressBar.isVisible = true
					binding.textViewPercent.isVisible = false
					binding.textViewDetails.isVisible = false
					binding.buttonCancel.isVisible = true
					binding.buttonResume.isVisible = false
				}

				is DownloadState.Progress -> {
					binding.textViewStatus.setText(R.string.manga_downloading_)
					binding.progressBar.isIndeterminate = false
					binding.progressBar.isVisible = true
					binding.progressBar.max = state.max
					binding.progressBar.setProgressCompat(state.progress, true)
					binding.textViewPercent.text = percentPattern.format((state.percent * 100f).format(1))
					binding.textViewPercent.isVisible = true
					binding.textViewDetails.isVisible = false
					binding.buttonCancel.isVisible = true
					binding.buttonResume.isVisible = false
				}

				is DownloadState.Queued -> {
					binding.textViewStatus.setText(R.string.queued)
					binding.progressBar.isIndeterminate = false
					binding.progressBar.isVisible = false
					binding.textViewPercent.isVisible = false
					binding.textViewDetails.isVisible = false
					binding.buttonCancel.isVisible = true
					binding.buttonResume.isVisible = false
				}
			}
		}.launchIn(lifecycleOwner.lifecycleScope)
	}

	onViewRecycled {
		job?.cancel()
		job = null
	}
}
