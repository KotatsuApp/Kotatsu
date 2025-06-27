package org.koitharu.kotatsu.core.nav

import android.app.ActivityOptions
import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import org.koitharu.kotatsu.core.util.ext.isAnimationsEnabled
import org.koitharu.kotatsu.core.util.ext.isOnScreen

inline val FragmentActivity.router: AppRouter
	get() = AppRouter(this)

inline val Fragment.router: AppRouter
	get() = AppRouter(this)

tailrec fun Fragment.dismissParentDialog(): Boolean {
	return when (val parent = parentFragment) {
		null -> return false
		is DialogFragment -> {
			parent.dismiss()
			true
		}

		else -> parent.dismissParentDialog()
	}
}

fun scaleUpActivityOptionsOf(view: View): Bundle? {
	if (!view.context.isAnimationsEnabled || !view.isOnScreen()) {
		return null
	}
	return ActivityOptions.makeScaleUpAnimation(
		/* source = */ view,
		/* startX = */ 0,
		/* startY = */ 0,
		/* width = */ view.width,
		/* height = */ view.height,
	).toBundle()
}
