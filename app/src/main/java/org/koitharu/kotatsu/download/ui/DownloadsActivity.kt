package org.koitharu.kotatsu.download.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.graphics.Insets
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import coil.ImageLoader
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koitharu.kotatsu.base.ui.BaseActivity
import org.koitharu.kotatsu.databinding.ActivityDownloadsBinding
import org.koitharu.kotatsu.download.ui.service.DownloadService
import org.koitharu.kotatsu.utils.bindServiceWithLifecycle

@AndroidEntryPoint
class DownloadsActivity : BaseActivity<ActivityDownloadsBinding>() {

	@Inject
	lateinit var coil: ImageLoader

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityDownloadsBinding.inflate(layoutInflater))
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		val adapter = DownloadsAdapter(lifecycleScope, coil)
		binding.recyclerView.setHasFixedSize(true)
		binding.recyclerView.adapter = adapter
		bindServiceWithLifecycle(
			owner = this,
			service = Intent(this, DownloadService::class.java),
			flags = 0,
		).service.flatMapLatest { binder ->
			(binder as? DownloadService.DownloadBinder)?.downloads ?: flowOf(null)
		}.onEach {
			adapter.items = it?.toList().orEmpty()
			binding.textViewHolder.isVisible = it.isNullOrEmpty()
		}.launchIn(lifecycleScope)
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		binding.root.updatePadding(
			left = insets.left,
			right = insets.right,
		)
		binding.recyclerView.updatePadding(
			bottom = insets.bottom,
		)
	}

	companion object {

		fun newIntent(context: Context) = Intent(context, DownloadsActivity::class.java)
	}
}
