package org.koitharu.kotatsu.filter.ui

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import androidx.core.widget.TextViewCompat
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.util.ext.drawableStart
import org.koitharu.kotatsu.core.util.ext.getThemeColorStateList
import org.koitharu.kotatsu.core.util.ext.setThemeTextAppearance
import org.koitharu.kotatsu.core.util.ext.textAndVisible
import org.koitharu.kotatsu.databinding.ViewFilterFieldBinding
import java.util.LinkedList
import androidx.appcompat.R as appcompatR
import com.google.android.material.R as materialR

class FilterFieldLayout @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	@AttrRes defStyleAttr: Int = 0,
) : RelativeLayout(context, attrs) {

	private val contentViews = LinkedList<View>()
	private val binding = ViewFilterFieldBinding.inflate(LayoutInflater.from(context), this)
	private var errorView: TextView? = null
	private var isInitialized = true

	init {
		context.withStyledAttributes(attrs, R.styleable.FilterFieldLayout, defStyleAttr) {
			binding.textViewTitle.text = getString(R.styleable.FilterFieldLayout_title)
			binding.buttonMore.isInvisible = !getBoolean(R.styleable.FilterFieldLayout_showMoreButton, false)
		}
	}

	override fun onViewAdded(child: View) {
		super.onViewAdded(child)
		if (!isInitialized) {
			return
		}
		assert(child.id != NO_ID)
		val lp = (child.layoutParams as? LayoutParams) ?: (generateDefaultLayoutParams() as LayoutParams)
		lp.alignWithParent = true
		lp.width = 0
		lp.addRule(ALIGN_PARENT_START)
		lp.addRule(ALIGN_PARENT_END)
		lp.addRule(BELOW, contentViews.lastOrNull()?.id ?: binding.textViewTitle.id)
		child.layoutParams = lp
		contentViews.add(child)
	}

	override fun onViewRemoved(child: View?) {
		super.onViewRemoved(child)
		contentViews.remove(child)
	}

	fun setValueText(valueText: String?) {
		if (!binding.buttonMore.isVisible) {
			binding.textViewValue.textAndVisible = valueText
		}
	}

	fun setTitle(@StringRes titleResId: Int) {
		binding.textViewTitle.setText(titleResId)
	}

	fun setError(errorMessage: String?) {
		if (errorMessage == null && errorView == null) {
			return
		}
		getErrorLabel().textAndVisible = errorMessage
	}

	fun setOnMoreButtonClickListener(clickListener: OnClickListener?) {
		binding.buttonMore.setOnClickListener(clickListener)
	}

	private fun getErrorLabel(): TextView {
		errorView?.let {
			return it
		}
		val label = TextView(context)
		label.id = R.id.textView_error
		label.compoundDrawablePadding = resources.getDimensionPixelOffset(R.dimen.screen_padding)
		label.gravity = Gravity.CENTER_VERTICAL or Gravity.START
		label.setPadding(resources.getDimensionPixelOffset(R.dimen.margin_small))
		label.setThemeTextAppearance(
			materialR.attr.textAppearanceBodySmall,
			materialR.style.TextAppearance_Material3_BodySmall,
		)
		label.drawableStart = ContextCompat.getDrawable(context, R.drawable.ic_error_small)
		TextViewCompat.setCompoundDrawableTintList(
			label,
			context.getThemeColorStateList(appcompatR.attr.colorControlNormal),
		)
		addView(label)
		errorView = label
		return label
	}
}
