package org.koitharu.kotatsu.tracker.ui.debug

import android.os.Bundle
import android.view.Gravity
import android.view.View
import androidx.activity.viewModels
import coil3.ImageLoader
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.core.nav.router
import org.koitharu.kotatsu.core.ui.BaseActivity
import org.koitharu.kotatsu.core.ui.BaseListAdapter
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.util.ext.consumeInsetsAsPadding
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.databinding.ActivityTrackerDebugBinding
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
			consumeInsetsAsPadding(Gravity.START or Gravity.END or Gravity.BOTTOM)
			setHasFixedSize(true)
			adapter = tracksAdapter
			addItemDecoration(TypedListSpacingDecoration(context, false))
		}
		viewModel.content.observe(this, tracksAdapter)
	}

	override fun onItemClick(item: TrackDebugItem, view: View) {
		router.openDetails(item.manga)
	}
}
