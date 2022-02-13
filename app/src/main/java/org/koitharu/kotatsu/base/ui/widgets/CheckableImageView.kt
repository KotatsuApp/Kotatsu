package org.koitharu.kotatsu.base.ui.widgets

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator
import android.util.AttributeSet
import android.widget.Checkable
import androidx.annotation.AttrRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.os.ParcelCompat

class CheckableImageView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	@AttrRes defStyleAttr: Int = 0,
) : AppCompatImageView(context, attrs, defStyleAttr), Checkable {

	private var isCheckedInternal = false
	private var isBroadcasting = false

	var onCheckedChangeListener: OnCheckedChangeListener? = null

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
			mergeDrawableStates(state, intArrayOf(android.R.attr.state_checked))
		}
		return state
	}

	override fun onSaveInstanceState(): Parcelable? {
		val superState = super.onSaveInstanceState() ?: return null
		return SavedState(superState, isChecked)
	}

	override fun onRestoreInstanceState(state: Parcelable?) {
		if (state is SavedState) {
			super.onRestoreInstanceState(state.superState)
			isChecked = state.isChecked
		} else {
			super.onRestoreInstanceState(state)
		}
	}

	fun interface OnCheckedChangeListener {

		fun onCheckedChanged(view: CheckableImageView, isChecked: Boolean)
	}

	private class SavedState : BaseSavedState {

		val isChecked: Boolean

		constructor(superState: Parcelable, checked: Boolean) : super(superState) {
			isChecked = checked
		}

		constructor(source: Parcel) : super(source) {
			isChecked = ParcelCompat.readBoolean(source)
		}

		override fun writeToParcel(out: Parcel, flags: Int) {
			super.writeToParcel(out, flags)
			ParcelCompat.writeBoolean(out, isChecked)
		}

		companion object {
			@JvmField
			val CREATOR: Creator<SavedState> = object : Creator<SavedState> {
				override fun createFromParcel(`in`: Parcel) = SavedState(`in`)

				override fun newArray(size: Int): Array<SavedState?> = arrayOfNulls(size)
			}
		}
	}
}