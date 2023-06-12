package org.koitharu.kotatsu.core.util

import android.app.Activity

class TaggedActivityResult(
	val tag: String,
	val result: Int,
) {

	val isSuccess: Boolean
		get() = result == Activity.RESULT_OK
}
