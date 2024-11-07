package org.koitharu.kotatsu.tracker.ui.debug

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.core.graphics.Insets
import androidx.core.view.updatePadding
import coil3.ImageLoader
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.core.ui.BaseActivity
import org.koitharu.kotatsu.core.ui.BaseListAdapter
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.databinding.ActivityTrackerDebugBinding
import org.koitharu.kotatsu.details.ui.DetailsActivity
import org.koitharu.kotatsu.list.ui.adapter.ListItemType
import org.koitharu.kotatsu.list.ui.adapter.TypedListSpacingDecoration
import javax.inject.Inject

@AndroidEntryPoint
class TrackerDebugActivity : BaseActivity<ActivityTrackerDebugBinding>(), OnListItemClickListener<TrackDebugItem> {

	@Inject
	lateinit var coil: ImageLoader

	private val viewModel by viewModels<TrackerDebugViewModel>()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityTrackerDebugBinding.inflate(layoutInflater))
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		val tracksAdapter = BaseListAdapter<TrackDebugItem>()
			.addDelegate(ListItemType.FEED, trackDebugAD(this, coil, this))
		with(viewBinding.recyclerView) {
			setHasFixedSize(true)
			adapter = tracksAdapter
			addItemDecoration(TypedListSpacingDecoration(context, false))
		}
		viewModel.content.observe(this, tracksAdapter)
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		val rv = viewBinding.recyclerView
		rv.updatePadding(
			left = insets.left + rv.paddingTop,
			right = insets.right + rv.paddingTop,
			bottom = insets.bottom,
		)
		viewBinding.toolbar.updatePadding(
			left = insets.left,
			right = insets.right,
		)
	}

	override fun onItemClick(item: TrackDebugItem, view: View) {
		startActivity(DetailsActivity.newIntent(this, item.manga))
	}
}
