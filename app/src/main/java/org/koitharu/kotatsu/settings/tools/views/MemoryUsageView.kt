package org.koitharu.kotatsu.settings.tools.views

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.core.graphics.ColorUtils
import androidx.core.widget.TextViewCompat
import com.google.android.material.color.MaterialColors
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.widgets.SegmentedBarView
import org.koitharu.kotatsu.databinding.LayoutMemoryUsageBinding
import org.koitharu.kotatsu.settings.tools.model.StorageUsage
import org.koitharu.kotatsu.utils.FileSize
import org.koitharu.kotatsu.utils.ext.getThemeColor

class MemoryUsageView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

	private val binding = LayoutMemoryUsageBinding.inflate(LayoutInflater.from(context), this)
	private val labelPattern = context.getString(R.string.memory_usage_pattern)

	init {
		orientation = VERTICAL
		bind(null)
	}

	fun setManageButtonOnClickListener(listener: OnClickListener?) {
		binding.buttonManage.setOnClickListener(listener)
	}

	fun bind(usage: StorageUsage?) {
		val storageSegment = SegmentedBarView.Segment(usage?.savedManga?.percent ?: 0f, segmentColor(1))
		val pagesSegment = SegmentedBarView.Segment(usage?.pagesCache?.percent ?: 0f, segmentColor(2))
		val otherSegment = SegmentedBarView.Segment(usage?.otherCache?.percent ?: 0f, segmentColor(3))

		with(binding) {
			bar.animateSegments(listOf(storageSegment, pagesSegment, otherSegment).filter { it.percent > 0f })
			labelStorage.text = formatLabel(usage?.savedManga, R.string.saved_manga)
			labelPagesCache.text = formatLabel(usage?.pagesCache, R.string.pages_cache)
			labelOtherCache.text = formatLabel(usage?.otherCache, R.string.other_cache)
			labelAvailable.text = formatLabel(usage?.available, R.string.available, R.string.available)

			TextViewCompat.setCompoundDrawableTintList(labelStorage, ColorStateList.valueOf(storageSegment.color))
			TextViewCompat.setCompoundDrawableTintList(labelPagesCache, ColorStateList.valueOf(pagesSegment.color))
			TextViewCompat.setCompoundDrawableTintList(labelOtherCache, ColorStateList.valueOf(otherSegment.color))
		}
	}

	private fun formatLabel(
		item: StorageUsage.Item?,
		@StringRes labelResId: Int,
		@StringRes emptyResId: Int = R.string.computing_,
	): String {
		return if (item != null) {
			labelPattern.format(
				FileSize.BYTES.format(context, item.bytes),
				context.getString(labelResId),
			)
		} else {
			context.getString(emptyResId)
		}
	}

	@ColorInt
	private fun segmentColor(i: Int): Int {
		val hue = (93.6f * i) % 360
		val color = ColorUtils.HSLToColor(floatArrayOf(hue, 0.4f, 0.6f))
		val backgroundColor = context.getThemeColor(com.google.android.material.R.attr.colorSecondaryContainer)
		return MaterialColors.harmonize(color, backgroundColor)
	}
}
