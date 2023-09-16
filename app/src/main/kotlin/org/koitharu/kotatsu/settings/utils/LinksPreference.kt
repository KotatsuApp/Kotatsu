package org.koitharu.kotatsu.settings.utils

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import androidx.core.text.method.LinkMovementMethodCompat
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder

class LinksPreference @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = androidx.preference.R.attr.preferenceStyle,
	defStyleRes: Int = 0,
) : Preference(context, attrs, defStyleAttr, defStyleRes) {
	override fun onBindViewHolder(holder: PreferenceViewHolder) {
		super.onBindViewHolder(holder)
		val summaryView = holder.findViewById(android.R.id.summary) as TextView
		summaryView.movementMethod = LinkMovementMethodCompat.getInstance()
	}
}
