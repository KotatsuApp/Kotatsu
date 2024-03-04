package org.koitharu.kotatsu.settings.userdata

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import androidx.annotation.StringRes
import androidx.core.widget.TextViewCompat
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import kotlinx.coroutines.flow.FlowCollector
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.widgets.SegmentedBarView
import org.koitharu.kotatsu.core.util.KotatsuColors
import org.koitharu.kotatsu.core.util.FileSize
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
			KotatsuColors.segmentColor(context, materialR.attr.colorPrimary),
		)
		val pagesSegment = SegmentedBarView.Segment(
			usage?.pagesCache?.percent ?: 0f,
			KotatsuColors.segmentColor(context, materialR.attr.colorSecondary),
		)
		val otherSegment = SegmentedBarView.Segment(
			usage?.otherCache?.percent ?: 0f,
			KotatsuColors.segmentColor(context, materialR.attr.colorTertiary),
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
}
