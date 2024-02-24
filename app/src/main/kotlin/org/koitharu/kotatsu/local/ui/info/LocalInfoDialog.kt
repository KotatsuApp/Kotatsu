package org.koitharu.kotatsu.local.ui.info

import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.core.graphics.ColorUtils
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.parcelable.ParcelableManga
import org.koitharu.kotatsu.core.ui.AlertDialogFragment
import org.koitharu.kotatsu.core.ui.widgets.SegmentedBarView
import org.koitharu.kotatsu.core.util.Colors
import org.koitharu.kotatsu.core.util.FileSize
import org.koitharu.kotatsu.core.util.ext.combine
import org.koitharu.kotatsu.core.util.ext.getThemeColor
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.showDistinct
import org.koitharu.kotatsu.core.util.ext.withArgs
import org.koitharu.kotatsu.databinding.DialogLocalInfoBinding
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.settings.userdata.StorageUsage
import com.google.android.material.R as materialR

@AndroidEntryPoint
class LocalInfoDialog : AlertDialogFragment<DialogLocalInfoBinding>() {

	private val viewModel: LocalInfoViewModel by viewModels()

	override fun onBuildDialog(builder: MaterialAlertDialogBuilder): MaterialAlertDialogBuilder {
		return super.onBuildDialog(builder)
			.setTitle(R.string.saved_manga)
			.setNegativeButton(R.string.close, null)
	}

	override fun onCreateViewBinding(inflater: LayoutInflater, container: ViewGroup?): DialogLocalInfoBinding {
		return DialogLocalInfoBinding.inflate(inflater, container, false)
	}

	override fun onViewBindingCreated(binding: DialogLocalInfoBinding, savedInstanceState: Bundle?) {
		super.onViewBindingCreated(binding, savedInstanceState)
		viewModel.path.observe(this) {
			binding.textViewPath.text = it
		}
		combine(viewModel.size, viewModel.availableSize, ::Pair).observe(this) {
			if (it.first >= 0 && it.second >= 0) {
				setSegments(it.first, it.second)
			} else {
				binding.barView.animateSegments(emptyList())
			}
		}
	}

	private fun setSegments(size: Long, available: Long) {
		val view = viewBinding?.barView ?: return
		val total = size + available
		val segment = SegmentedBarView.Segment(
			percent = (size.toDouble() / total.toDouble()).toFloat(),
			color = Colors.segmentColor(view.context, materialR.attr.colorPrimary),
		)
		requireViewBinding().labelUsed.text = view.context.getString(
			R.string.memory_usage_pattern,
			getString(R.string.this_manga),
			FileSize.BYTES.format(view.context, size),
		)
		requireViewBinding().labelAvailable.text = view.context.getString(
			R.string.memory_usage_pattern,
			getString(R.string.available),
			FileSize.BYTES.format(view.context, available),
		)
		TextViewCompat.setCompoundDrawableTintList(
			requireViewBinding().labelUsed,
			ColorStateList.valueOf(segment.color),
		)
		view.animateSegments(listOf(segment))
	}

	companion object {

		const val ARG_MANGA = "manga"
		private const val TAG = "LocalInfoDialog"

		fun show(fm: FragmentManager, manga: Manga) {
			LocalInfoDialog().withArgs(1) {
				putParcelable(ARG_MANGA, ParcelableManga(manga))
			}.showDistinct(fm, TAG)
		}
	}
}
