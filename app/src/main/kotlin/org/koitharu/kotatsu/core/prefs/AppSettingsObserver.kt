package org.koitharu.kotatsu.core.prefs

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform

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

fun <T> AppSettings.observeAsStateFlow(
	scope: CoroutineScope,
	key: String,
	valueProducer: AppSettings.() -> T,
): StateFlow<T> = observe().transform {
	if (it == key) {
		emit(valueProducer())
	}
}.stateIn(scope, SharingStarted.Eagerly, valueProducer())
