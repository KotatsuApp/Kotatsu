package org.koitharu.kotatsu.core.ui.widgets

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator
import android.util.AttributeSet
import androidx.core.content.withStyledAttributes
import androidx.customview.view.AbsSavedState
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.android.material.textview.MaterialTextView
import org.koitharu.kotatsu.R

class BadgeView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null
) : MaterialTextView(context, attrs, R.attr.badgeViewStyle) {

	private var maxCharacterCount = Int.MAX_VALUE

	var number: Int = 0
		set(value) {
			field = value
			updateText()
		}

	init {
		context.withStyledAttributes(attrs, R.styleable.BadgeView, R.attr.badgeViewStyle) {
			maxCharacterCount = getInt(R.styleable.BadgeView_maxCharacterCount, maxCharacterCount)
			number = getInt(R.styleable.BadgeView_number, number)
			val shape = ShapeAppearanceModel.builder(
				context,
				getResourceId(R.styleable.BadgeView_shapeAppearance, 0),
				0,
			).build()
			background = MaterialShapeDrawable(shape).also { bg ->
				bg.fillColor = getColorStateList(R.styleable.BadgeView_backgroundColor)
			}
		}
	}

	override fun onSaveInstanceState(): Parcelable? {
		val superState = super.onSaveInstanceState() ?: return null
		return SavedState(superState, number)
	}

	override fun onRestoreInstanceState(state: Parcelable?) {
		if (state is SavedState) {
			super.onRestoreInstanceState(state.superState)
			number = state.number
		} else {
			super.onRestoreInstanceState(state)
		}
	}

	private fun updateText() {
		if (number <= 0) {
			text = null
			return
		}
		val numberString = number.toString()
		text = if (numberString.length > maxCharacterCount) {
			buildString(maxCharacterCount) {
				repeat(maxCharacterCount - 1) { append('9') }
				append('+')
			}
		} else {
			numberString
		}
	}

	private class SavedState : AbsSavedState {

		val number: Int

		constructor(superState: Parcelable, number: Int) : super(superState) {
			this.number = number
		}

		constructor(source: Parcel, classLoader: ClassLoader?) : super(source, classLoader) {
			number = source.readInt()
		}

		override fun writeToParcel(out: Parcel, flags: Int) {
			super.writeToParcel(out, flags)
			out.writeInt(number)
		}

		companion object {
			@Suppress("unused")
			@JvmField
			val CREATOR: Creator<SavedState> = object : Creator<SavedState> {
				override fun createFromParcel(`in`: Parcel) = SavedState(`in`, SavedState::class.java.classLoader)

				override fun newArray(size: Int): Array<SavedState?> = arrayOfNulls(size)
			}
		}
	}
}
