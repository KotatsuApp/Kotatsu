package org.koitharu.kotatsu.settings.utils

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.preference.PreferenceViewHolder
import androidx.preference.SwitchPreferenceCompat
import org.koitharu.kotatsu.R

class SplitSwitchPreference @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = androidx.preference.R.attr.switchPreferenceCompatStyle,
	defStyleRes: Int = 0
) : SwitchPreferenceCompat(context, attrs, defStyleAttr, defStyleRes) {

	init {
		layoutResource = R.layout.preference_split_switch
	}

	var onContainerClickListener: OnPreferenceClickListener? = null

	private val containerClickListener = View.OnClickListener {
		onContainerClickListener?.onPreferenceClick(this)
	}

	override fun onBindViewHolder(holder: PreferenceViewHolder) {
		super.onBindViewHolder(holder)
		holder.findViewById(R.id.press_container)?.setOnClickListener(containerClickListener)
	}

}
