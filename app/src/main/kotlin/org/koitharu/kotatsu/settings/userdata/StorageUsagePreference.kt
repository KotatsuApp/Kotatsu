package org.koitharu.kotatsu.settings.userdata

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.core.graphics.ColorUtils
import androidx.core.widget.TextViewCompat
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.google.android.material.color.MaterialColors
import kotlinx.coroutines.flow.FlowCollector
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.widgets.SegmentedBarView
import org.koitharu.kotatsu.core.util.FileSize
import org.koitharu.kotatsu.core.util.ext.getThemeColor
import org.koitharu.kotatsu.databinding.PreferenceMemoryUsageBinding
import com.google.android.material.R as materialR

class StorageUsagePreference @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
) : Preference(context, attrs), FlowCollector<StorageUsage?> {

	private val labelPattern = context.getString(R.string.memory_usage_pattern)
	private var usage: StorageUsage? = null

	init {
		layoutResource = R.layout.preference_memory_usage
		isSelectable = false
		isPersistent = false
	}

	override fun onBindViewHolder(holder: PreferenceViewHolder) {
		super.onBindViewHolder(holder)
		val binding = PreferenceMemoryUsageBinding.bind(holder.itemView)
		val storageSegment = SegmentedBarView.Segment(
			usage?.savedManga?.percent ?: 0f,
			segmentColor(materialR.attr.colorPrimary),
		)
		val pagesSegment = SegmentedBarView.Segment(
			usage?.pagesCache?.percent ?: 0f,
			segmentColor(materialR.attr.colorSecondary),
		)
		val otherSegment = SegmentedBarView.Segment(
			usage?.otherCache?.percent ?: 0f,
			segmentColor(materialR.attr.colorTertiary),
		)

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

	override suspend fun emit(value: StorageUsage?) {
		usage = value
		notifyChanged()
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
		val backgroundColor = context.getThemeColor(materialR.attr.colorSurfaceContainerHigh)
		return MaterialColors.harmonize(color, backgroundColor)
	}
}
