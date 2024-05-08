package org.koitharu.kotatsu.core.util.ext

import android.os.Looper

fun Throwable.printStackTraceDebug() = printStackTrace()

fun assertNotInMainThread() = check(Looper.myLooper() != Looper.getMainLooper()) {
	"Calling this from the main thread is prohibited"
}
