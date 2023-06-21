package org.koitharu.kotatsu.core.ui.widgets

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.TypedArray
import android.graphics.Color
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.annotation.AttrRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
import androidx.core.view.updateLayoutParams
import androidx.core.widget.ImageViewCompat
import androidx.core.widget.TextViewCompat
import com.google.android.material.ripple.RippleUtils
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.util.ext.resolveDp
import org.koitharu.kotatsu.core.util.ext.textAndVisible
import org.koitharu.kotatsu.databinding.ViewTwoLinesItemBinding

@SuppressLint("RestrictedApi")
class TwoLinesItemView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	@AttrRes defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

	private val binding = ViewTwoLinesItemBinding.inflate(LayoutInflater.from(context), this)

	var title: CharSequence?
		get() = binding.title.text
		set(value) {
			binding.title.text = value
		}

	var subtitle: CharSequence?
		get() = binding.subtitle.textAndVisible
		set(value) {
			binding.subtitle.textAndVisible = value
		}

	init {
		var textColors: ColorStateList? = null
		context.withStyledAttributes(
			set = attrs,
			attrs = R.styleable.TwoLinesItemView,
			defStyleAttr = defStyleAttr,
			defStyleRes = R.style.Widget_Kotatsu_TwoLinesItemView,
		) {
			val itemRippleColor = getRippleColor(context)
			val shape = createShapeDrawable(this)
			val roundCorners = FloatArray(8) { resources.resolveDp(16f) }
			background = RippleDrawable(
				RippleUtils.sanitizeRippleDrawableColor(itemRippleColor),
				shape,
				ShapeDrawable(RoundRectShape(roundCorners, null, null)),
			)
			val drawablePadding = getDimensionPixelOffset(R.styleable.TwoLinesItemView_android_drawablePadding, 0)
			binding.layoutText.updateLayoutParams<MarginLayoutParams> { marginStart = drawablePadding }
			setIconResource(getResourceId(R.styleable.TwoLinesItemView_icon, 0))
			binding.title.text = getText(R.styleable.TwoLinesItemView_title)
			binding.subtitle.text = getText(R.styleable.TwoLinesItemView_subtitle)
			textColors = getColorStateList(R.styleable.TwoLinesItemView_android_textColor)
			val textAppearanceFallback = androidx.appcompat.R.style.TextAppearance_AppCompat
			TextViewCompat.setTextAppearance(
				binding.title,
				getResourceId(R.styleable.TwoLinesItemView_titleTextAppearance, textAppearanceFallback),
			)
			TextViewCompat.setTextAppearance(
				binding.subtitle,
				getResourceId(R.styleable.TwoLinesItemView_subtitleTextAppearance, textAppearanceFallback),
			)
		}
		if (textColors == null) {
			textColors = binding.title.textColors
		}
		binding.title.setTextColor(textColors)
		binding.subtitle.setTextColor(textColors)
		ImageViewCompat.setImageTintList(binding.icon, textColors)
	}

	fun setIconResource(@DrawableRes resId: Int) {
		binding.icon.setImageResource(resId)
	}

	private fun createShapeDrawable(ta: TypedArray): InsetDrawable {
		val shapeAppearance = ShapeAppearanceModel.builder(
			context,
			ta.getResourceId(R.styleable.TwoLinesItemView_shapeAppearance, 0),
			ta.getResourceId(R.styleable.TwoLinesItemView_shapeAppearanceOverlay, 0),
		).build()
		val shapeDrawable = MaterialShapeDrawable(shapeAppearance)
		shapeDrawable.fillColor = ta.getColorStateList(R.styleable.TwoLinesItemView_backgroundFillColor)
		return InsetDrawable(
			shapeDrawable,
			ta.getDimensionPixelOffset(R.styleable.TwoLinesItemView_android_insetLeft, 0),
			ta.getDimensionPixelOffset(R.styleable.TwoLinesItemView_android_insetTop, 0),
			ta.getDimensionPixelOffset(R.styleable.TwoLinesItemView_android_insetRight, 0),
			ta.getDimensionPixelOffset(R.styleable.TwoLinesItemView_android_insetBottom, 0),
		)
	}

	private fun getRippleColor(context: Context): ColorStateList {
		return ContextCompat.getColorStateList(context, R.color.selector_overlay)
			?: ColorStateList.valueOf(Color.TRANSPARENT)
	}
}
