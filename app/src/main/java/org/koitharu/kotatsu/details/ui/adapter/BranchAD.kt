package org.koitharu.kotatsu.details.ui.adapter

import android.graphics.Color
import android.text.Spannable
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import androidx.core.text.buildSpannedString
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.list.AdapterDelegateClickListenerAdapter
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.util.ext.getThemeColor
import org.koitharu.kotatsu.databinding.ItemCheckableNewBinding
import org.koitharu.kotatsu.details.ui.model.MangaBranch

fun branchAD(
	clickListener: OnListItemClickListener<MangaBranch>,
) = adapterDelegateViewBinding<MangaBranch, MangaBranch, ItemCheckableNewBinding>(
	{ inflater, parent -> ItemCheckableNewBinding.inflate(inflater, parent, false) },
) {

	val clickAdapter = AdapterDelegateClickListenerAdapter(this, clickListener)
	itemView.setOnClickListener(clickAdapter)
	val counterColorSpan = ForegroundColorSpan(context.getThemeColor(android.R.attr.textColorSecondary, Color.LTGRAY))
	val counterSizeSpan = RelativeSizeSpan(0.86f)

	bind {
		binding.root.text = buildSpannedString {
			append(item.name ?: getString(R.string.system_default))
			append(' ')
			append(' ')
			val start = length
			append(item.count.toString())
			setSpan(counterColorSpan, start, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
			setSpan(counterSizeSpan, start, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
		}
		binding.root.isChecked = item.isSelected
	}
}
