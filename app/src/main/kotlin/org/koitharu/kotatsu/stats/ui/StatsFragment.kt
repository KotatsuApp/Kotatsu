package org.koitharu.kotatsu.stats.ui

import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.graphics.drawable.shapes.Shape
import android.os.Bundle
import android.text.style.DynamicDrawableSpan
import android.text.style.ImageSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.Insets
import androidx.core.text.buildSpannedString
import androidx.core.text.inSpans
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.BaseFragment
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.databinding.FragmentStatsBinding
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.stats.domain.StatsRecord
import org.koitharu.kotatsu.stats.ui.views.PieChartView

@AndroidEntryPoint
class StatsFragment : BaseFragment<FragmentStatsBinding>() {

	private val viewModel: StatsViewModel by viewModels()

	override fun onCreateViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentStatsBinding {
		return FragmentStatsBinding.inflate(inflater, container, false)
	}

	override fun onViewBindingCreated(binding: FragmentStatsBinding, savedInstanceState: Bundle?) {
		super.onViewBindingCreated(binding, savedInstanceState)
		viewModel.readingStats.observe(viewLifecycleOwner) {
			val sum = it.sumOf { it.duration }
			binding.chart.setData(
				it.map { v ->
					PieChartView.Segment(
						value = (v.duration / 1000).toInt(),
						label = v.manga.title,
						percent = (v.duration.toDouble() / sum).toFloat(),
						color = v.getColor(binding.chart.context),
					)
				},
			)
			binding.textViewLegend.text = buildLegend(it)
		}
	}

	override fun onWindowInsetsChanged(insets: Insets) = Unit

	private fun buildLegend(stats: List<StatsRecord>) = buildSpannedString {
		val context = context ?: return@buildSpannedString
		for (item in stats) {
			ContextCompat.getDrawable(context, R.drawable.bg_rounded_square)?.let { icon ->
				icon.setBounds(0, 0, icon.intrinsicWidth, icon.intrinsicHeight)
				icon.setTint(item.getColor(context))
				inSpans(ImageSpan(icon, DynamicDrawableSpan.ALIGN_BASELINE)) {
					append(' ')
				}
				append(' ')
			}
			append(item.manga.title)
			append(" - ")
			append(item.time.format(context.resources))
			appendLine()
		}
	}
}
