package org.koitharu.kotatsu.ui.common.widgets

import android.content.Context
import android.util.AttributeSet
import android.widget.Checkable
import androidx.appcompat.widget.AppCompatImageView

class CheckableImageView @JvmOverloads constructor(
	context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr), Checkable {

	private var isCheckedInternal = false
	private var isBroadcasting = false

	var onCheckedChangeListener: OnCheckedChangeListener? = null

	init {
		setOnClickListener {
			toggle()
		}
	}

	fun setOnCheckedChangeListener(listener: (Boolean) -> Unit) {
		onCheckedChangeListener = object : OnCheckedChangeListener {
			override fun onCheckedChanged(view: CheckableImageView, isChecked: Boolean) {
				listener(isChecked)
			}
		}
	}

	override fun isChecked() = isCheckedInternal

	override fun toggle() {
		isChecked = !isCheckedInternal
	}

	override fun setChecked(checked: Boolean) {
		if (checked != isCheckedInternal) {
			isCheckedInternal = checked
			refreshDrawableState()
			if (!isBroadcasting) {
				isBroadcasting = true
				onCheckedChangeListener?.onCheckedChanged(this, checked)
				isBroadcasting = false
			}
		}
	}

	override fun onCreateDrawableState(extraSpace: Int): IntArray {
		val state = super.onCreateDrawableState(extraSpace + 1)
		if (isCheckedInternal) {
			mergeDrawableStates(state, CHECKED_STATE_SET)
		}
		return state
	}

	interface OnCheckedChangeListener {

		fun onCheckedChanged(view: CheckableImageView, isChecked: Boolean)
	}

	private companion object {

		@JvmStatic
		private val CHECKED_STATE_SET = intArrayOf(android.R.attr.state_checked)
	}
}