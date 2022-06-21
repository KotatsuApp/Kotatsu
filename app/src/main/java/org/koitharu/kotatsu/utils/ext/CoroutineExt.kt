package org.koitharu.kotatsu.utils.ext

import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope

val processLifecycleScope: LifecycleCoroutineScope
	inline get() = ProcessLifecycleOwner.get().lifecycleScope