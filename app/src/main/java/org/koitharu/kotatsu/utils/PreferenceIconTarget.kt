package org.koitharu.kotatsu.utils

import android.graphics.drawable.Drawable
import androidx.preference.Preference
import coil.target.Target

class PreferenceIconTarget(
	private val preference: Preference,
) : Target {

	override fun onError(error: Drawable?) {
		preference.icon = error
	}

	override fun onStart(placeholder: Drawable?) {
		preference.icon = placeholder
	}

	override fun onSuccess(result: Drawable) {
		preference.icon = result
	}
}