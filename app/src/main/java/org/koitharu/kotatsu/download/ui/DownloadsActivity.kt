package org.koitharu.kotatsu.download.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.core.graphics.Insets
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import coil.ImageLoader
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseActivity
import org.koitharu.kotatsu.base.ui.list.decor.SpacingItemDecoration
import org.koitharu.kotatsu.databinding.ActivityDownloadsBinding
import org.koitharu.kotatsu.download.ui.service.DownloadService

@AndroidEntryPoint
class DownloadsActivity : BaseActivity<ActivityDownloadsBinding>() {

	@Inject
	lateinit var coil: ImageLoader

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityDownloadsBinding.inflate(layoutInflater))
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		val adapter = DownloadsAdapter(lifecycleScope, coil)
		val spacing = resources.getDimensionPixelOffset(R.dimen.grid_spacing)
		binding.recyclerView.addItemDecoration(SpacingItemDecoration(spacing))
		binding.recyclerView.setHasFixedSize(true)
		binding.recyclerView.adapter = adapter
		val connection = DownloadServiceConnection(adapter)
		bindService(Intent(this, DownloadService::class.java), connection, 0)
		lifecycle.addObserver(connection)
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

	private inner class DownloadServiceConnection(
		private val adapter: DownloadsAdapter,
	) : ServiceConnection, DefaultLifecycleObserver {

		private var collectJob: Job? = null

		override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
			collectJob?.cancel()
			val binder = (service as? DownloadService.DownloadBinder)
			collectJob = if (binder == null) {
				null
			} else {
				lifecycleScope.launch {
					binder.downloads.collect {
						setItems(it)
					}
				}
			}
		}

		override fun onServiceDisconnected(name: ComponentName?) {
			collectJob?.cancel()
			collectJob = null
			setItems(null)
		}

		override fun onDestroy(owner: LifecycleOwner) {
			super.onDestroy(owner)
			collectJob?.cancel()
			collectJob = null
			owner.lifecycle.removeObserver(this)
			unbindService(this)
		}

		private fun setItems(items: Collection<DownloadItem>?) {
			adapter.items = items?.toList().orEmpty()
			binding.textViewHolder.isVisible = items.isNullOrEmpty()
		}
	}

	companion object {

		fun newIntent(context: Context) = Intent(context, DownloadsActivity::class.java)
	}
}
