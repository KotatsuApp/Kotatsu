package org.koitharu.kotatsu.base.ui.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.AttrRes
import androidx.annotation.IdRes
import androidx.core.view.children
import com.google.android.material.R as materialR
import com.google.android.material.button.MaterialButton
import com.google.android.material.shape.ShapeAppearanceModel

@Deprecated("")
class CheckableButtonGroup @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	@AttrRes defStyleAttr: Int = materialR.attr.materialButtonToggleGroupStyle,
) : LinearLayout(context, attrs, defStyleAttr, materialR.style.Widget_MaterialComponents_MaterialButtonToggleGroup),
	View.OnClickListener {

	private val originalCornerData = ArrayList<CornerData>()

	var onCheckedChangeListener: OnCheckedChangeListener? = null

	override fun addView(child: View?, index: Int, params: ViewGroup.LayoutParams?) {
		if (child is MaterialButton) {
			setupButton(child)
		}
		super.addView(child, index, params)
	}

	override fun onFinishInflate() {
		super.onFinishInflate()
		updateChildShapes()
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

	private fun updateChildShapes() {
		val childCount = childCount
		val firstVisibleChildIndex = 0
		val lastVisibleChildIndex = childCount - 1
		for (i in 0 until childCount) {
			val button: MaterialButton = getChildAt(i) as? MaterialButton ?: continue
			if (button.visibility == GONE) {
				continue
			}
			val builder = button.shapeAppearanceModel.toBuilder()
			val newCornerData: CornerData? =
				getNewCornerData(i, firstVisibleChildIndex, lastVisibleChildIndex)
			updateBuilderWithCornerData(builder, newCornerData)
			button.shapeAppearanceModel = builder.build()
		}
	}

	private fun setupButton(button: MaterialButton) {
		button.setOnClickListener(this)
		button.isElegantTextHeight = false
		// Saves original corner data
		val shapeAppearanceModel: ShapeAppearanceModel = button.shapeAppearanceModel
		originalCornerData.add(
			CornerData(
				shapeAppearanceModel.topLeftCornerSize,
				shapeAppearanceModel.bottomLeftCornerSize,
				shapeAppearanceModel.topRightCornerSize,
				shapeAppearanceModel.bottomRightCornerSize,
			),
		)
	}

	private fun getNewCornerData(
		index: Int,
		firstVisibleChildIndex: Int,
		lastVisibleChildIndex: Int,
	): CornerData? {
		val cornerData: CornerData = originalCornerData.get(index)

		// If only one (visible) child exists, use its original corners
		if (firstVisibleChildIndex == lastVisibleChildIndex) {
			return cornerData
		}
		val isHorizontal = orientation == HORIZONTAL
		if (index == firstVisibleChildIndex) {
			return if (isHorizontal) cornerData.start(this) else cornerData.top()
		}
		return if (index == lastVisibleChildIndex) {
			if (isHorizontal) cornerData.end(this) else cornerData.bottom()
		} else null
	}

	private fun updateBuilderWithCornerData(
		shapeAppearanceModelBuilder: ShapeAppearanceModel.Builder,
		cornerData: CornerData?,
	) {
		if (cornerData == null) {
			shapeAppearanceModelBuilder.setAllCornerSizes(0f)
			return
		}
		shapeAppearanceModelBuilder
			.setTopLeftCornerSize(cornerData.topLeft)
			.setBottomLeftCornerSize(cornerData.bottomLeft)
			.setTopRightCornerSize(cornerData.topRight)
			.setBottomRightCornerSize(cornerData.bottomRight)
	}

	fun interface OnCheckedChangeListener {
		fun onCheckedChanged(group: CheckableButtonGroup, checkedId: Int)
	}
}
