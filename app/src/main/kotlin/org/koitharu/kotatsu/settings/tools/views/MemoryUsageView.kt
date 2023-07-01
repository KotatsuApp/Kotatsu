package org.koitharu.kotatsu.settings.tools.views

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.widget.TextViewCompat
import com.google.android.material.color.MaterialColors
import okio.ByteString.Companion.decodeHex
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.widgets.SegmentedBarView
import org.koitharu.kotatsu.core.util.FileSize
import org.koitharu.kotatsu.core.util.ext.getThemeColor
import org.koitharu.kotatsu.databinding.LayoutMemoryUsageBinding
import org.koitharu.kotatsu.settings.tools.model.StorageUsage


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
		val storageSegment = SegmentedBarView.Segment(usage?.savedManga?.percent ?: 0f, segmentColor(com.google.android.material.R.attr.colorPrimary))
		val pagesSegment = SegmentedBarView.Segment(usage?.pagesCache?.percent ?: 0f, segmentColor(com.google.android.material.R.attr.colorOnPrimaryContainer))
		val otherSegment = SegmentedBarView.Segment(usage?.otherCache?.percent ?: 0f, segmentColor(com.google.android.material.R.attr.colorTertiary))

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

	private fun getHue(hex: String): Float {
		val r = (hex.substring(0, 2).toInt(16)).toFloat()
		val g = (hex.substring(2, 4).toInt(16)).toFloat()
		val b = (hex.substring(4, 6).toInt(16)).toFloat()

		var hue = 0F
		if ((r >= g) && (g >= b)) {
			hue = 60 * (g - b) / (r - b)
		} else if ((g > r) && (r >= b)) {
			hue = 60 * (2 - (r - b) / (g - b))
		}
		return hue
	}

	@ColorInt
	private fun segmentColor(@AttrRes resId: Int): Int {
		val colorHex = String.format("%06x", context.getThemeColor(resId))
		val hue = getHue(colorHex)
		val color = ColorUtils.HSLToColor(floatArrayOf(hue, 0.5f, 0.5f))
		val backgroundColor = context.getThemeColor(com.google.android.material.R.attr.colorSurfaceContainerHigh)
		return MaterialColors.harmonize(color, backgroundColor)
	}
}
