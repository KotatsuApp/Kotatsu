package org.koitharu.kotatsu.search.ui.widget

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import androidx.annotation.AttrRes
import com.google.android.material.R
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.shape.MaterialShapeDrawable

class SearchToolbar @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	@AttrRes defStyleAttr: Int = R.attr.toolbarStyle,
) : MaterialToolbar(context, attrs, defStyleAttr) {

	private val bgDrawable = MaterialShapeDrawable(context, attrs, defStyleAttr, 0)

	init {
		bgDrawable.initializeElevationOverlay(context)
		bgDrawable.setShadowColor(Color.DKGRAY)
		background = bgDrawable
	}
}