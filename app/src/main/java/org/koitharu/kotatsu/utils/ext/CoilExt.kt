package org.koitharu.kotatsu.utils.ext

import androidx.core.graphics.drawable.toBitmap
import coil.request.ErrorResult
import coil.request.RequestResult
import coil.request.SuccessResult

fun RequestResult.requireBitmap() = when(this) {
	is SuccessResult -> drawable.toBitmap()
	is ErrorResult -> throw throwable
}

fun RequestResult.toBitmapOrNull() = when(this) {
	is SuccessResult -> try {
		drawable.toBitmap()
	} catch (_: Throwable) {
		null
	}
	is ErrorResult -> null
}