package org.koitharu.kotatsu.settings.search

import androidx.preference.PreferenceFragmentCompat
import org.koitharu.kotatsu.list.ui.model.ListModel

data class SettingsItem(
	val key: String,
	val title: CharSequence,
	val breadcrumbs: List<String>,
	val fragmentClass: Class<out PreferenceFragmentCompat>,
) : ListModel {

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is SettingsItem && other.key == key
	}
}
