package org.koitharu.kotatsu.core.ui.widgets

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.TypedArray
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import android.util.AttributeSet
import androidx.annotation.AttrRes
import androidx.appcompat.widget.AppCompatCheckedTextView
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
import com.google.android.material.ripple.RippleUtils
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.util.ext.resolveDp

@SuppressLint("RestrictedApi")
class ListItemTextView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	@AttrRes defStyleAttr: Int = R.attr.listItemTextViewStyle,
) : AppCompatCheckedTextView(context, attrs, defStyleAttr) {

	private var checkedDrawableStart: Drawable? = null
	private var checkedDrawableEnd: Drawable? = null
	private var isInitialized = false
	private var isCheckDrawablesVisible: Boolean = false
	private var defaultPaddingStart: Int = 0
	private var defaultPaddingEnd: Int = 0

	init {
		context.withStyledAttributes(attrs, R.styleable.ListItemTextView, defStyleAttr) {
			val itemRippleColor = getRippleColor(context)
			val shape = createShapeDrawable(this)
			val roundCorners = FloatArray(8) { resources.resolveDp(32f) }
			background = RippleDrawable(
				RippleUtils.sanitizeRippleDrawableColor(itemRippleColor),
				shape,
				ShapeDrawable(RoundRectShape(roundCorners, null, null)),
			)
			checkedDrawableStart = getDrawable(R.styleable.ListItemTextView_checkedDrawableStart)
			checkedDrawableEnd = getDrawable(R.styleable.ListItemTextView_checkedDrawableEnd)
		}
		checkedDrawableStart?.setTintList(textColors)
		checkedDrawableEnd?.setTintList(textColors)
		defaultPaddingStart = paddingStart
		defaultPaddingEnd = paddingEnd
		isInitialized = true
		adjustCheckDrawables()
	}

	override fun refreshDrawableState() {
		super.refreshDrawableState()
		adjustCheckDrawables()
	}

	override fun setTextColor(colors: ColorStateList?) {
		checkedDrawableStart?.setTintList(colors)
		checkedDrawableEnd?.setTintList(colors)
		super.setTextColor(colors)
	}

	override fun setPaddingRelative(start: Int, top: Int, end: Int, bottom: Int) {
		defaultPaddingStart = start
		defaultPaddingEnd = end
		super.setPaddingRelative(start, top, end, bottom)
	}

	override fun setPadding(left: Int, top: Int, right: Int, bottom: Int) {
		val isRtl = layoutDirection == LAYOUT_DIRECTION_RTL
		defaultPaddingStart = if (isRtl) right else left
		defaultPaddingEnd = if (isRtl) left else right
		super.setPadding(left, top, right, bottom)
	}

	private fun adjustCheckDrawables() {
		if (isInitialized && isCheckDrawablesVisible != isChecked) {
			setCompoundDrawablesRelativeWithIntrinsicBounds(
				if (isChecked) checkedDrawableStart else null,
				null,
				if (isChecked) checkedDrawableEnd else null,
				null,
			)
			super.setPaddingRelative(
				if (isChecked && checkedDrawableStart != null) {
					defaultPaddingStart + compoundDrawablePadding
				} else defaultPaddingStart,
				paddingTop,
				if (isChecked && checkedDrawableEnd != null) {
					defaultPaddingEnd + compoundDrawablePadding
				} else defaultPaddingEnd,
				paddingBottom,
			)
			isCheckDrawablesVisible = isChecked
		}
	}

	private fun createShapeDrawable(ta: TypedArray): InsetDrawable {
		val shapeAppearance = ShapeAppearanceModel.builder(
			context,
			ta.getResourceId(R.styleable.ListItemTextView_shapeAppearance, 0),
			ta.getResourceId(R.styleable.ListItemTextView_shapeAppearanceOverlay, 0),
		).build()
		val shapeDrawable = MaterialShapeDrawable(shapeAppearance)
		shapeDrawable.fillColor = ta.getColorStateList(R.styleable.ListItemTextView_backgroundFillColor)
		return InsetDrawable(
			shapeDrawable,
			ta.getDimensionPixelOffset(R.styleable.ListItemTextView_android_insetLeft, 0),
			ta.getDimensionPixelOffset(R.styleable.ListItemTextView_android_insetTop, 0),
			ta.getDimensionPixelOffset(R.styleable.ListItemTextView_android_insetRight, 0),
			ta.getDimensionPixelOffset(R.styleable.ListItemTextView_android_insetBottom, 0),
		)
	}

	private fun getRippleColor(context: Context): ColorStateList {
		return ContextCompat.getColorStateList(context, R.color.selector_overlay)
			?: ColorStateList.valueOf(Color.TRANSPARENT)
	}
}
