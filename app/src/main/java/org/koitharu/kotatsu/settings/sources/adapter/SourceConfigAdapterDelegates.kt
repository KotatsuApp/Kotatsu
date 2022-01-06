package org.koitharu.kotatsu.settings.sources.adapter

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.widget.CompoundButton
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.databinding.ItemExpandableBinding
import org.koitharu.kotatsu.databinding.ItemSourceConfigBinding

fun sourceConfigHeaderDelegate(
	listener: SourceConfigListener,
) = adapterDelegateViewBinding<SourceConfigItem.LocaleHeader, SourceConfigItem, ItemExpandableBinding>(
	{ layoutInflater, parent -> ItemExpandableBinding.inflate(layoutInflater, parent, false) }
) {

	binding.root.setOnClickListener {
		listener.onHeaderClick(item)
	}

	bind {
		binding.root.text = item.title ?: getString(R.string.other)
		binding.root.isChecked = item.isExpanded
	}
}

@SuppressLint("ClickableViewAccessibility")
fun sourceConfigItemDelegate(
	listener: SourceConfigListener,
) = adapterDelegateViewBinding<SourceConfigItem.SourceItem, SourceConfigItem, ItemSourceConfigBinding>(
	{ layoutInflater, parent -> ItemSourceConfigBinding.inflate(layoutInflater, parent, false) }
) {

	val eventListener = object : View.OnClickListener, View.OnTouchListener,
		CompoundButton.OnCheckedChangeListener {
		override fun onClick(v: View?) = listener.onItemSettingsClick(item)

		override fun onTouch(v: View?, event: MotionEvent): Boolean {
			return if (event.actionMasked == MotionEvent.ACTION_DOWN) {
				listener.onDragHandleTouch(this@adapterDelegateViewBinding)
				true
			} else {
				false
			}
		}

		override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
			listener.onItemEnabledChanged(item, isChecked)
		}
	}

	binding.imageViewConfig.setOnClickListener(eventListener)
	binding.switchToggle.setOnCheckedChangeListener(eventListener)
	binding.imageViewHandle.setOnTouchListener(eventListener)

	bind {
		binding.textViewTitle.text = item.source.title
		binding.switchToggle.isChecked = item.isEnabled
	}
}