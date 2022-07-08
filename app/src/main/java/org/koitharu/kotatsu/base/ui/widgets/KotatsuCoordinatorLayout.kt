package org.koitharu.kotatsu.base.ui.widgets

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.doOnLayout
import androidx.customview.view.AbsSavedState
import com.google.android.material.appbar.AppBarLayout
import org.koitharu.kotatsu.utils.ext.findChild

class KotatsuCoordinatorLayout @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = androidx.coordinatorlayout.R.attr.coordinatorLayoutStyle
) : CoordinatorLayout(context, attrs, defStyleAttr) {

	private var appBarLayout: AppBarLayout? = null

	/**
	 * If true, [AppBarLayout] child will be lifted on nested scroll.
	 */
	var isLiftAppBarOnScroll = true

	/**
	 * Internal check
	 */
	private val canLiftAppBarOnScroll
		get() = isLiftAppBarOnScroll

	override fun onNestedScroll(
		target: View,
		dxConsumed: Int,
		dyConsumed: Int,
		dxUnconsumed: Int,
		dyUnconsumed: Int,
		type: Int,
		consumed: IntArray
	) {
		super.onNestedScroll(target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type, consumed)
		if (canLiftAppBarOnScroll) {
			appBarLayout?.isLifted = dyConsumed != 0 || dyUnconsumed >= 0
		}
	}

	override fun onAttachedToWindow() {
		super.onAttachedToWindow()
		appBarLayout = findChild()
	}

	override fun onDetachedFromWindow() {
		super.onDetachedFromWindow()
		appBarLayout = null
	}

	override fun onSaveInstanceState(): Parcelable? {
		val superState = super.onSaveInstanceState()
		return if (superState != null) {
			SavedState(superState).also {
				it.appBarLifted = appBarLayout?.isLifted ?: false
			}
		} else {
			superState
		}
	}

	override fun onRestoreInstanceState(state: Parcelable?) {
		if (state is SavedState) {
			super.onRestoreInstanceState(state.superState)
			doOnLayout {
				appBarLayout?.isLifted = state.appBarLifted
			}
		} else {
			super.onRestoreInstanceState(state)
		}
	}

	internal class SavedState : AbsSavedState {
		var appBarLifted = false

		constructor(superState: Parcelable) : super(superState)

		constructor(source: Parcel, loader: ClassLoader?) : super(source, loader) {
			appBarLifted = source.readByte().toInt() == 1
		}

		override fun writeToParcel(out: Parcel, flags: Int) {
			super.writeToParcel(out, flags)
			out.writeByte((if (appBarLifted) 1 else 0).toByte())
		}

		companion object {
			@JvmField
			val CREATOR: Parcelable.ClassLoaderCreator<SavedState> = object : Parcelable.ClassLoaderCreator<SavedState> {
				override fun createFromParcel(source: Parcel, loader: ClassLoader): SavedState {
					return SavedState(source, loader)
				}

				override fun createFromParcel(source: Parcel): SavedState {
					return SavedState(source, null)
				}

				override fun newArray(size: Int): Array<SavedState> {
					return newArray(size)
				}
			}
		}
	}
}