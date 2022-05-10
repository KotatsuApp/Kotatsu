package org.koitharu.kotatsu.core.prefs

import androidx.lifecycle.liveData
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.flow.flow

fun <T> AppSettings.observeAsFlow(key: String, valueProducer: AppSettings.() -> T) = flow {
	var lastValue: T = valueProducer()
	emit(lastValue)
	observe().collect {
		if (it == key) {
			val value = valueProducer()
			if (value != lastValue) {
				emit(value)
			}
			lastValue = value
		}
	}
}

fun <T> AppSettings.observeAsLiveData(
	context: CoroutineContext,
	key: String,
	valueProducer: AppSettings.() -> T
) = liveData(context) {
	emit(valueProducer())
	observe().collect {
		if (it == key) {
			val value = valueProducer()
			if (value != latestValue) {
				emit(value)
			}
		}
	}
}