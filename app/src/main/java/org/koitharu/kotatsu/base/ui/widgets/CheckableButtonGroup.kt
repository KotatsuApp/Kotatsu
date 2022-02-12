package org.koitharu.kotatsu.base.ui.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.AttrRes
import androidx.annotation.IdRes
import androidx.core.view.children
import com.google.android.material.button.MaterialButton

class CheckableButtonGroup @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	@AttrRes defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr), View.OnClickListener {

	var onCheckedChangeListener: OnCheckedChangeListener? = null

	override fun addView(child: View?, index: Int, params: ViewGroup.LayoutParams?) {
		if (child is MaterialButton) {
			child.setOnClickListener(this)
		}
		super.addView(child, index, params)
	}

	override fun onClick(v: View) {
		setCheckedId(v.id)
	}

	fun setCheckedId(@IdRes viewRes: Int) {
		children.forEach {
			(it as? MaterialButton)?.isChecked = it.id == viewRes
		}
		onCheckedChangeListener?.onCheckedChanged(this, viewRes)
	}

	fun interface OnCheckedChangeListener {
		fun onCheckedChanged(group: CheckableButtonGroup, checkedId: Int)
	}
}