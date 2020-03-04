package org.koitharu.kotatsu.ui.reader.wetoon

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class WebtoonRecyclerView @JvmOverloads constructor(
	context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {

	override fun onNestedPreScroll(target: View?, dx: Int, dy: Int, consumed: IntArray?) {
		super.onNestedPreScroll(target, dx, dy, consumed)
	}
}