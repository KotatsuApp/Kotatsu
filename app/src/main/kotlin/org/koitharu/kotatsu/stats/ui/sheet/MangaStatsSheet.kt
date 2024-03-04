package org.koitharu.kotatsu.stats.ui.sheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.collection.IntList
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.parcelable.ParcelableManga
import org.koitharu.kotatsu.core.ui.sheet.BaseAdaptiveSheet
import org.koitharu.kotatsu.core.util.KotatsuColors
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.showDistinct
import org.koitharu.kotatsu.core.util.ext.textAndVisible
import org.koitharu.kotatsu.core.util.ext.withArgs
import org.koitharu.kotatsu.databinding.SheetStatsMangaBinding
import org.koitharu.kotatsu.details.ui.DetailsActivity
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.util.format
import org.koitharu.kotatsu.stats.ui.views.BarChartView

@AndroidEntryPoint
class MangaStatsSheet : BaseAdaptiveSheet<SheetStatsMangaBinding>(), View.OnClickListener {

	private val viewModel: MangaStatsViewModel by viewModels()

	override fun onCreateViewBinding(inflater: LayoutInflater, container: ViewGroup?): SheetStatsMangaBinding {
		return SheetStatsMangaBinding.inflate(inflater, container, false)
	}

	override fun onViewBindingCreated(binding: SheetStatsMangaBinding, savedInstanceState: Bundle?) {
		super.onViewBindingCreated(binding, savedInstanceState)
		binding.textViewTitle.text = viewModel.manga.title
		binding.chartView.barColor = KotatsuColors.ofManga(binding.root.context, viewModel.manga)
		viewModel.stats.observe(viewLifecycleOwner, ::onStatsChanged)
		viewModel.startDate.observe(viewLifecycleOwner) {
			binding.textViewStart.textAndVisible = it?.format(resources)
		}
		viewModel.totalPagesRead.observe(viewLifecycleOwner) {
			binding.textViewPages.text = getString(R.string.pages_read_s, it.format())
		}
		binding.buttonOpen.setOnClickListener(this)
	}

	override fun onClick(v: View) {
		startActivity(DetailsActivity.newIntent(v.context, viewModel.manga))
	}

	private fun onStatsChanged(stats: IntList) {
		val chartView = viewBinding?.chartView ?: return
		if (stats.isEmpty()) {
			chartView.setData(emptyList())
			return
		}
		val bars = ArrayList<BarChartView.Bar>(stats.size)
		stats.forEach { pages ->
			bars.add(
				BarChartView.Bar(
					value = pages,
					label = pages.toString(),
				),
			)
		}
		chartView.setData(bars)
	}

	companion object {

		const val ARG_MANGA = "manga"

		private const val TAG = "MangaStatsSheet"

		fun show(fm: FragmentManager, manga: Manga) {
			MangaStatsSheet().withArgs(1) {
				putParcelable(ARG_MANGA, ParcelableManga(manga))
			}.showDistinct(fm, TAG)
		}
	}
}
