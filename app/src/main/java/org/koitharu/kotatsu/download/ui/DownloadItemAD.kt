package org.koitharu.kotatsu.download.ui

import androidx.core.view.isVisible
import coil.ImageLoader
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.databinding.ItemDownloadBinding
import org.koitharu.kotatsu.download.domain.DownloadManager
import org.koitharu.kotatsu.utils.ext.*
import org.koitharu.kotatsu.utils.progress.ProgressJob

fun downloadItemAD(
	scope: CoroutineScope,
	coil: ImageLoader,
) = adapterDelegateViewBinding<ProgressJob<DownloadManager.State>, ProgressJob<DownloadManager.State>, ItemDownloadBinding>(
	{ inflater, parent -> ItemDownloadBinding.inflate(inflater, parent, false) }
) {

	var job: Job? = null

	bind {
		job?.cancel()
		job = item.progressAsFlow().onFirst { state ->
			binding.imageViewCover.newImageRequest(state.manga.coverUrl)
				.referer(state.manga.publicUrl)
				.placeholder(state.cover)
				.fallback(R.drawable.ic_placeholder)
				.error(R.drawable.ic_placeholder)
				.allowRgb565(true)
				.enqueueWith(coil)
		}.onEach { state ->
			binding.textViewTitle.text = state.manga.title
			when (state) {
				is DownloadManager.State.Cancelling -> {
					binding.textViewStatus.setText(R.string.cancelling_)
					binding.progressBar.setIndeterminateCompat(true)
					binding.progressBar.isVisible = true
					binding.textViewPercent.isVisible = false
					binding.textViewDetails.isVisible = false
				}
				is DownloadManager.State.Done -> {
					binding.textViewStatus.setText(R.string.download_complete)
					binding.progressBar.setIndeterminateCompat(false)
					binding.progressBar.isVisible = false
					binding.textViewPercent.isVisible = false
					binding.textViewDetails.isVisible = false
				}
				is DownloadManager.State.Error -> {
					binding.textViewStatus.setText(R.string.error_occurred)
					binding.progressBar.setIndeterminateCompat(false)
					binding.progressBar.isVisible = false
					binding.textViewPercent.isVisible = false
					binding.textViewDetails.text = state.error.getDisplayMessage(context.resources)
					binding.textViewDetails.isVisible = true
				}
				is DownloadManager.State.PostProcessing -> {
					binding.textViewStatus.setText(R.string.processing_)
					binding.progressBar.setIndeterminateCompat(true)
					binding.progressBar.isVisible = true
					binding.textViewPercent.isVisible = false
					binding.textViewDetails.isVisible = false
				}
				is DownloadManager.State.Preparing -> {
					binding.textViewStatus.setText(R.string.preparing_)
					binding.progressBar.setIndeterminateCompat(true)
					binding.progressBar.isVisible = true
					binding.textViewPercent.isVisible = false
					binding.textViewDetails.isVisible = false
				}
				is DownloadManager.State.Progress -> {
					binding.textViewStatus.setText(R.string.manga_downloading_)
					binding.progressBar.setIndeterminateCompat(false)
					binding.progressBar.isVisible = true
					binding.progressBar.max = state.max
					binding.progressBar.setProgressCompat(state.progress, true)
					binding.textViewPercent.text = (state.percent * 100f).format(1) + "%"
					binding.textViewPercent.isVisible = true
					binding.textViewDetails.isVisible = false
				}
				is DownloadManager.State.Queued -> {
					binding.textViewStatus.setText(R.string.queued)
					binding.progressBar.setIndeterminateCompat(false)
					binding.progressBar.isVisible = false
					binding.textViewPercent.isVisible = false
					binding.textViewDetails.isVisible = false
				}
				is DownloadManager.State.WaitingForNetwork -> {
					binding.textViewStatus.setText(R.string.waiting_for_network)
					binding.progressBar.setIndeterminateCompat(false)
					binding.progressBar.isVisible = false
					binding.textViewPercent.isVisible = false
					binding.textViewDetails.isVisible = false
				}
			}
		}.launchIn(scope)
	}

	onViewRecycled {
		job?.cancel()
		job = null
	}
}