package org.koitharu.kotatsu.base.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koitharu.kotatsu.utils.ext.processLifecycleScope

fun interface ReversibleHandle {

	suspend fun reverse()
}

fun ReversibleHandle.reverseAsync() = processLifecycleScope.launch(Dispatchers.Default) {
	reverse()
}

operator fun ReversibleHandle.plus(other: ReversibleHandle) = ReversibleHandle {
	this.reverse()
	other.reverse()
}