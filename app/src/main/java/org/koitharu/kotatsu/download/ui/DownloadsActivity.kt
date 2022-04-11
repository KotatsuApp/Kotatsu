package org.koitharu.kotatsu.download.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import androidx.core.graphics.Insets
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.android.ext.android.get
import org.koitharu.kotatsu.base.ui.BaseActivity
import org.koitharu.kotatsu.databinding.ActivityDownloadsBinding
import org.koitharu.kotatsu.download.ui.service.DownloadService
import org.koitharu.kotatsu.utils.LifecycleAwareServiceConnection

class DownloadsActivity : BaseActivity<ActivityDownloadsBinding>() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityDownloadsBinding.inflate(layoutInflater))
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		val adapter = DownloadsAdapter(lifecycleScope, get())
		binding.recyclerView.setHasFixedSize(true)
		binding.recyclerView.adapter = adapter
		LifecycleAwareServiceConnection.bindService(
			this,
			this,
			Intent(this, DownloadService::class.java),
			0
		).service.flatMapLatest { binder ->
			(binder as? DownloadService.DownloadBinder)?.downloads ?: flowOf(null)
		}.onEach {
			adapter.items = it?.toList().orEmpty()
			binding.textViewHolder.isVisible = it.isNullOrEmpty()
		}.launchIn(lifecycleScope)
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		binding.recyclerView.updatePadding(
			left = insets.left,
			right = insets.right,
			bottom = insets.bottom
		)
		binding.toolbar.updatePadding(
			left = insets.left,
			right = insets.right
		)
	}

	companion object {

		fun newIntent(context: Context) = Intent(context, DownloadsActivity::class.java)
	}
}