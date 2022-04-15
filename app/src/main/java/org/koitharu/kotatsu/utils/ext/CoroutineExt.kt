package org.koitharu.kotatsu.utils.ext

import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineExceptionHandler
import org.koitharu.kotatsu.BuildConfig

val IgnoreErrors
	get() = CoroutineExceptionHandler { _, e ->
		if (BuildConfig.DEBUG) {
			e.printStackTrace()
		}
	}

val processLifecycleScope: LifecycleCoroutineScope
	inline get() = ProcessLifecycleOwner.get().lifecycleScope