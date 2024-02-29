package org.koitharu.kotatsu.stats.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.core.graphics.Insets
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.BaseActivity
import org.koitharu.kotatsu.core.ui.BaseFragment
import org.koitharu.kotatsu.core.ui.BaseListAdapter
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.databinding.ActivityStatsBinding
import org.koitharu.kotatsu.details.ui.DetailsActivity
import org.koitharu.kotatsu.list.ui.adapter.ListItemType
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.stats.domain.StatsRecord
import org.koitharu.kotatsu.stats.ui.views.PieChartView

@AndroidEntryPoint
class StatsActivity : BaseActivity<ActivityStatsBinding>(), OnListItemClickListener<Manga> {

	private val viewModel: StatsViewModel by viewModels()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityStatsBinding.inflate(layoutInflater))
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		val adapter = BaseListAdapter<StatsRecord>()
			.addDelegate(ListItemType.FEED, statsAD(this))
		viewBinding.recyclerView.adapter = adapter
		viewModel.readingStats.observe(this) {
			val sum = it.sumOf { it.duration }
			viewBinding.chart.setData(
				it.map { v ->
					PieChartView.Segment(
						value = (v.duration / 1000).toInt(),
						label = v.manga?.title ?: getString(R.string.other_manga),
						percent = (v.duration.toDouble() / sum).toFloat(),
						color = v.getColor(this),
					)
				},
			)
			adapter.emit(it)
		}
	}

	override fun onWindowInsetsChanged(insets: Insets) = Unit

	override fun onItemClick(item: Manga, view: View) {
		startActivity(DetailsActivity.newIntent(view.context, item))
	}
}
