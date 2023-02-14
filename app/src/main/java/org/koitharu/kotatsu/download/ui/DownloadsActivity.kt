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
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseActivity
import org.koitharu.kotatsu.base.ui.list.decor.SpacingItemDecoration
import org.koitharu.kotatsu.databinding.ActivityDownloadsBinding
import javax.inject.Inject

@AndroidEntryPoint
class DownloadsActivity : BaseActivity<ActivityDownloadsBinding>() {

	@Inject
	lateinit var coil: ImageLoader

	private lateinit var serviceConnection: DownloadsConnection

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityDownloadsBinding.inflate(layoutInflater))
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		val adapter = DownloadsAdapter(lifecycleScope, coil)
		val spacing = resources.getDimensionPixelOffset(R.dimen.list_spacing)
		binding.recyclerView.addItemDecoration(SpacingItemDecoration(spacing))
		binding.recyclerView.setHasFixedSize(true)
		binding.recyclerView.adapter = adapter
		serviceConnection = DownloadsConnection(this, this)
		serviceConnection.items.observe(this) { items ->
			adapter.items = items
			binding.textViewHolder.isVisible = items.isNullOrEmpty()
		}
		serviceConnection.bind()
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

	companion object {

		fun newIntent(context: Context) = Intent(context, DownloadsActivity::class.java)
	}
}
