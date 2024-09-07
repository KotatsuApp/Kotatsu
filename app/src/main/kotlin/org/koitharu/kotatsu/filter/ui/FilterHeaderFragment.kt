package org.koitharu.kotatsu.filter.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.Insets
import androidx.core.view.isVisible
import com.google.android.material.chip.Chip
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.BaseFragment
import org.koitharu.kotatsu.core.ui.widgets.ChipsView
import org.koitharu.kotatsu.core.util.ext.isAnimationsEnabled
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.databinding.FragmentFilterHeaderBinding
import org.koitharu.kotatsu.filter.ui.model.FilterHeaderModel
import org.koitharu.kotatsu.filter.ui.tags.TagsCatalogSheet
import org.koitharu.kotatsu.parsers.model.MangaTag
import com.google.android.material.R as materialR

class FilterHeaderFragment : BaseFragment<FragmentFilterHeaderBinding>(), ChipsView.OnChipClickListener {

	private val filter: MangaFilter
		get() = (requireActivity() as FilterOwner).filter

	override fun onCreateViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentFilterHeaderBinding {
		return FragmentFilterHeaderBinding.inflate(inflater, container, false)
	}

	override fun onViewBindingCreated(binding: FragmentFilterHeaderBinding, savedInstanceState: Bundle?) {
		super.onViewBindingCreated(binding, savedInstanceState)
		binding.chipsTags.onChipClickListener = this
		filter.header.observe(viewLifecycleOwner, ::onDataChanged)
	}

	override fun onWindowInsetsChanged(insets: Insets) = Unit

	override fun onChipClick(chip: Chip, data: Any?) {
		val tag = data as? MangaTag
		if (tag == null) {
			TagsCatalogSheet.show(parentFragmentManager, isExcludeTag = false)
		} else {
			filter.setTag(tag, !chip.isChecked)
		}
	}

	private fun onDataChanged(header: FilterHeaderModel) {
		val binding = viewBinding ?: return
		val chips = header.chips
		if (chips.isEmpty()) {
			binding.chipsTags.setChips(emptyList())
			binding.root.isVisible = false
			return
		}
		if (binding.root.context.isAnimationsEnabled) {
			binding.scrollView.smoothScrollTo(0, 0)
		} else {
			binding.scrollView.scrollTo(0, 0)
		}
		binding.chipsTags.setChips(header.chips + moreTagsChip())
		binding.root.isVisible = true
	}

	private fun moreTagsChip() = ChipsView.ChipModel(
		title = getString(R.string.more),
		icon = materialR.drawable.abc_ic_menu_overflow_material,
	)
}
