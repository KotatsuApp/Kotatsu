package org.koitharu.kotatsu.base.ui.util

import androidx.lifecycle.MutableLiveData

class CountedBooleanLiveData : MutableLiveData<Boolean>(false) {

	private var counter = 0

	override fun setValue(value: Boolean) {
		if (value) {
			counter++
		} else {
			counter--
		}
		val newValue = counter > 0
		if (newValue != this.value) {
			super.setValue(newValue)
		}
	}
}