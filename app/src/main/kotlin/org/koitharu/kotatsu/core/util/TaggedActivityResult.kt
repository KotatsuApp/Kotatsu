package org.koitharu.kotatsu.core.util

import android.app.Activity

class TaggedActivityResult(
	val tag: String,
	val result: Int,
)

val TaggedActivityResult.isSuccess: Boolean
	get() = this.result == Activity.RESULT_OK
