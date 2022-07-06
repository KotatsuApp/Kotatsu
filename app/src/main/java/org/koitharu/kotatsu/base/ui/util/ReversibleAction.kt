package org.koitharu.kotatsu.base.ui.util

import androidx.annotation.StringRes
import org.koitharu.kotatsu.base.domain.ReversibleHandle

class ReversibleAction(
	@StringRes val stringResId: Int,
	val handle: ReversibleHandle?,
)