package org.koitharu.kotatsu.utils.anim

import androidx.annotation.IntegerRes

enum class Duration(@IntegerRes val resId: Int) {
	SHORT(android.R.integer.config_shortAnimTime),
	MEDIUM(android.R.integer.config_mediumAnimTime),
	LONG(android.R.integer.config_longAnimTime)
}