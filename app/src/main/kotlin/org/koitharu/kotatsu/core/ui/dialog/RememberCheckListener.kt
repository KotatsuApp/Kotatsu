package org.koitharu.kotatsu.core.ui.dialog

import android.widget.CompoundButton
import android.widget.CompoundButton.OnCheckedChangeListener

class RememberCheckListener(
	initialValue: Boolean,
) : OnCheckedChangeListener {

	var isChecked: Boolean = initialValue
		private set

	override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
		this.isChecked = isChecked
	}
}
