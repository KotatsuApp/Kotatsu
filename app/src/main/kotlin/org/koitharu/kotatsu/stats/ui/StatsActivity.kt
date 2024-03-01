package org.koitharu.kotatsu.stats.ui

import android.graphics.Color
import android.os.Bundle
import android.text.style.DynamicDrawableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.text.style.RelativeSizeSpan
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.widget.PopupMenu
import androidx.core.graphics.Insets
import androidx.core.text.buildSpannedString
import androidx.core.text.inSpans
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.AsyncListDiffer
import coil.ImageLoader
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.BaseActivity
import org.koitharu.kotatsu.core.ui.BaseFragment
import org.koitharu.kotatsu.core.ui.BaseListAdapter
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.ui.util.ReversibleActionObserver
import org.koitharu.kotatsu.core.util.ext.DIALOG_THEME_CENTERED
import org.koitharu.kotatsu.core.util.ext.enqueueWith
import org.koitharu.kotatsu.core.util.ext.getThemeColor
import org.koitharu.kotatsu.core.util.ext.newImageRequest
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.observeEvent
import org.koitharu.kotatsu.core.util.ext.setTextAndVisible
import org.koitharu.kotatsu.core.util.ext.showOrHide
import org.koitharu.kotatsu.databinding.ActivityStatsBinding
import org.koitharu.kotatsu.databinding.ItemEmptyStateBinding
import org.koitharu.kotatsu.details.ui.DetailsActivity
import org.koitharu.kotatsu.list.ui.adapter.ListItemType
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.stats.domain.StatsPeriod
import org.koitharu.kotatsu.stats.domain.StatsRecord
import org.koitharu.kotatsu.stats.ui.views.PieChartView
import javax.inject.Inject

@AndroidEntryPoint
class StatsActivity : BaseActivity<ActivityStatsBinding>(),
	OnListItemClickListener<Manga>,
	PieChartView.OnSegmentClickListener,
	AsyncListDiffer.ListListener<StatsRecord>,
	ViewStub.OnInflateListener {

	@Inject
	lateinit var coil: ImageLoader

	private val viewModel: StatsViewModel by viewModels()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityStatsBinding.inflate(layoutInflater))
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		val adapter = BaseListAdapter<StatsRecord>()
			.addDelegate(ListItemType.FEED, statsAD(this))
			.addListListener(this)
		viewBinding.recyclerView.adapter = adapter
		viewBinding.chart.onSegmentClickListener = this
		viewBinding.stubEmpty.setOnInflateListener(this)

		viewModel.isLoading.observe(this) {
			viewBinding.progressBar.showOrHide(it)
		}
		viewModel.period.observe(this) {
			supportActionBar?.setSubtitle(it.titleResId)
		}
		viewModel.onActionDone.observeEvent(this, ReversibleActionObserver(viewBinding.recyclerView))
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

	override fun onSegmentClick(view: PieChartView, segment: PieChartView.Segment) {
		Toast.makeText(this, segment.label, Toast.LENGTH_SHORT).apply {
			setGravity(Gravity.TOP, 0, view.top + view.height / 2)
		}.show()
	}

	override fun onCreateOptionsMenu(menu: Menu?): Boolean {
		menuInflater.inflate(R.menu.opt_stats, menu)
		return super.onCreateOptionsMenu(menu)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.action_clear -> {
				showClearConfirmDialog()
				true
			}

			R.id.action_period -> {
				showPeriodSelector()
				true
			}

			else -> super.onOptionsItemSelected(item)
		}
	}

	override fun onCurrentListChanged(previousList: MutableList<StatsRecord>, currentList: MutableList<StatsRecord>) {
		val isEmpty = currentList.isEmpty()
		with(viewBinding) {
			chart.isGone = isEmpty
			recyclerView.isGone = isEmpty
			stubEmpty.isVisible = isEmpty
		}
	}

	override fun onInflate(stub: ViewStub?, inflated: View) {
		val stubBinding = ItemEmptyStateBinding.bind(inflated)
		stubBinding.icon.newImageRequest(this, R.drawable.ic_empty_history)?.enqueueWith(coil)
		stubBinding.textPrimary.setText(R.string.text_empty_holder_primary)
		stubBinding.textSecondary.setTextAndVisible(R.string.empty_stats_text)
		stubBinding.buttonRetry.isVisible = false
	}

	private fun showClearConfirmDialog() {
		MaterialAlertDialogBuilder(this, DIALOG_THEME_CENTERED)
			.setMessage(R.string.clear_stats_confirm)
			.setTitle(R.string.clear_stats)
			.setIcon(R.drawable.ic_delete)
			.setNegativeButton(android.R.string.cancel, null)
			.setPositiveButton(R.string.clear) { _, _ ->
				viewModel.clear()
			}.show()
	}

	private fun showPeriodSelector() {
		val menu = PopupMenu(this, viewBinding.toolbar)
		for ((i, branch) in StatsPeriod.entries.withIndex()) {
			menu.menu.add(Menu.NONE, Menu.NONE, i, branch.titleResId)
		}
		menu.setOnMenuItemClickListener {
			StatsPeriod.entries.getOrNull(it.order)?.also {
				viewModel.period.value = it
			} != null
		}
		menu.show()
	}
}
