package org.koitharu.kotatsu.utils.ext

import org.koitharu.kotatsu.BuildConfig

inline fun <T, R> T.safe(action: T.() -> R?) = try {
	this.action()
} catch (e: Exception) {
	if (BuildConfig.DEBUG) {
		e.printStackTrace()
	}
	null
}