package org.koitharu.kotatsu.core.prefs

import androidx.lifecycle.liveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform
import kotlin.coroutines.CoroutineContext

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
	valueProducer: AppSettings.() -> T,
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

fun <T> AppSettings.observeAsStateFlow(
	key: String,
	scope: CoroutineScope,
	valueProducer: AppSettings.() -> T,
): StateFlow<T> = observe().transform {
	if (it == key) {
		emit(valueProducer())
	}
}.stateIn(scope, SharingStarted.Eagerly, valueProducer())
