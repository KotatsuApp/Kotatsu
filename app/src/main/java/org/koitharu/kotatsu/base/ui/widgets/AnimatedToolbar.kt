package org.koitharu.kotatsu.base.ui.widgets

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isGone
import com.google.android.material.R
import com.google.android.material.appbar.MaterialToolbar
import java.lang.reflect.Field

class AnimatedToolbar @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = R.attr.toolbarStyle,
) : MaterialToolbar(context, attrs, defStyleAttr) {

	private var navButtonView: View? = null
		get() {
			if (field == null) {
				runCatching {
					field = navButtonViewField?.get(this) as? View
				}
			}
			return field
		}

	override fun setNavigationIcon(icon: Drawable?) {
		super.setNavigationIcon(icon)
		navButtonView?.isGone = (icon == null)
	}

	private companion object {

		val navButtonViewField: Field? = runCatching {
			Toolbar::class.java.getDeclaredField("mNavButtonView")
				.also { it.isAccessible = true }
		}.getOrNull()
	}
}