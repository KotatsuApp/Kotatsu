package org.koitharu.kotatsu.download.ui.list

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.core.graphics.Insets
import androidx.core.view.updatePadding
import coil.ImageLoader
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseActivity
import org.koitharu.kotatsu.base.ui.list.decor.SpacingItemDecoration
import org.koitharu.kotatsu.databinding.ActivityDownloadsBinding
import org.koitharu.kotatsu.details.ui.DetailsActivity
import org.koitharu.kotatsu.download.ui.worker.PausingReceiver
import javax.inject.Inject

@AndroidEntryPoint
class DownloadsActivity : BaseActivity<ActivityDownloadsBinding>(), DownloadItemListener {

	@Inject
	lateinit var coil: ImageLoader

	private val viewModel by viewModels<DownloadsViewModel>()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityDownloadsBinding.inflate(layoutInflater))
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		val adapter = DownloadsAdapter(this, coil, this)
		val spacing = resources.getDimensionPixelOffset(R.dimen.list_spacing)
		binding.recyclerView.addItemDecoration(SpacingItemDecoration(spacing))
		binding.recyclerView.setHasFixedSize(true)
		binding.recyclerView.adapter = adapter

		viewModel.items.observe(this) {
			adapter.items = it
		}
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		binding.recyclerView.updatePadding(
			left = insets.left,
			right = insets.right,
			bottom = insets.bottom,
		)
		binding.toolbar.updatePadding(
			left = insets.left,
			right = insets.right,
		)
	}

	override fun onItemClick(item: DownloadItemModel, view: View) {
		startActivity(DetailsActivity.newIntent(view.context, item.manga))
	}

	override fun onCancelClick(item: DownloadItemModel) {
		viewModel.cancel(item.id)
	}

	override fun onPauseClick(item: DownloadItemModel) {
		sendBroadcast(PausingReceiver.getPauseIntent(item.id))
	}

	override fun onResumeClick(item: DownloadItemModel) {
		sendBroadcast(PausingReceiver.getResumeIntent(item.id))
	}

	override fun onRetryClick(item: DownloadItemModel) {
		// TODO
	}

	companion object {

		fun newIntent(context: Context) = Intent(context, DownloadsActivity::class.java)
	}
}
