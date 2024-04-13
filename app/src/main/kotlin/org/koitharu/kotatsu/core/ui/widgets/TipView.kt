package org.koitharu.kotatsu.core.ui.widgets

import android.content.Context
import android.graphics.Outline
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.util.ext.drawableStart
import org.koitharu.kotatsu.core.util.ext.getDrawableCompat
import org.koitharu.kotatsu.core.util.ext.getThemeColorStateList
import org.koitharu.kotatsu.core.util.ext.setTextAndVisible
import org.koitharu.kotatsu.core.util.ext.textAndVisible
import org.koitharu.kotatsu.databinding.ViewTipBinding

class TipView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = R.attr.tipViewStyle,
) : LinearLayout(context, attrs, defStyleAttr), View.OnClickListener {

	private val binding = ViewTipBinding.inflate(LayoutInflater.from(context), this)

	var title: CharSequence?
		get() = binding.textViewTitle.text
		set(value) {
			binding.textViewTitle.text = value
		}

	var text: CharSequence?
		get() = binding.textViewBody.text
		set(value) {
			binding.textViewBody.text = value
		}

	var icon: Drawable?
		get() = binding.textViewTitle.drawableStart
		set(value) {
			binding.textViewTitle.drawableStart = value
		}

	var primaryButtonText: CharSequence?
		get() = binding.buttonPrimary.textAndVisible
		set(value) {
			binding.buttonPrimary.textAndVisible = value
		}

	var secondaryButtonText: CharSequence?
		get() = binding.buttonSecondary.textAndVisible
		set(value) {
			binding.buttonSecondary.textAndVisible = value
		}

	var onButtonClickListener: OnButtonClickListener? = null

	init {
		orientation = VERTICAL
		setPadding(context.resources.getDimensionPixelOffset(R.dimen.margin_normal))
		context.withStyledAttributes(attrs, R.styleable.TipView, defStyleAttr) {
			title = getText(R.styleable.TipView_title)
			text = getText(R.styleable.TipView_android_text)
			icon = getDrawableCompat(context, R.styleable.TipView_icon)
			primaryButtonText = getString(R.styleable.TipView_primaryButtonText)
			secondaryButtonText = getString(R.styleable.TipView_secondaryButtonText)
			val shapeAppearanceModel = ShapeAppearanceModel.builder(context, attrs, defStyleAttr, 0).build()
			background = MaterialShapeDrawable(shapeAppearanceModel).also {
				it.fillColor = getColorStateList(R.styleable.TipView_cardBackgroundColor)
					?: context.getThemeColorStateList(com.google.android.material.R.attr.colorSurfaceContainerHigh)
				it.strokeWidth = getDimension(R.styleable.TipView_strokeWidth, 0f)
				it.strokeColor = getColorStateList(R.styleable.TipView_strokeColor)
				it.elevation = getDimension(R.styleable.TipView_elevation, 0f)
			}
			outlineProvider = OutlineProvider(shapeAppearanceModel)
		}
		binding.buttonPrimary.setOnClickListener(this)
		binding.buttonSecondary.setOnClickListener(this)
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.button_primary -> onButtonClickListener?.onPrimaryButtonClick(this)
			R.id.button_secondary -> onButtonClickListener?.onSecondaryButtonClick(this)
		}
	}

	fun setTitle(@StringRes resId: Int) {
		binding.textViewTitle.setText(resId)
	}

	fun setText(@StringRes resId: Int) {
		binding.textViewBody.setText(resId)
	}

	fun setPrimaryButtonText(@StringRes resId: Int) {
		binding.buttonPrimary.setTextAndVisible(resId)
		updateButtonsLayout()
	}

	fun setSecondaryButtonText(@StringRes resId: Int) {
		binding.buttonSecondary.setTextAndVisible(resId)
		updateButtonsLayout()
	}

	fun setIcon(@DrawableRes resId: Int) {
		icon = ContextCompat.getDrawable(context, resId)
	}

	private fun updateButtonsLayout() {
		binding.layoutButtons.isVisible = binding.buttonPrimary.isVisible || binding.buttonSecondary.isVisible
	}

	interface OnButtonClickListener {

		fun onPrimaryButtonClick(tipView: TipView)

		fun onSecondaryButtonClick(tipView: TipView)
	}

	private class OutlineProvider(
		shapeAppearanceModel: ShapeAppearanceModel,
	) : ViewOutlineProvider() {

		private val shapeDrawable = MaterialShapeDrawable(shapeAppearanceModel)
		override fun getOutline(view: View, outline: Outline) {
			shapeDrawable.setBounds(0, 0, view.width, view.height)
			shapeDrawable.getOutline(outline)
		}
	}

}
