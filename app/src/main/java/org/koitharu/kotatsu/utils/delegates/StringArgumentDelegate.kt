package org.koitharu.kotatsu.utils.delegates

import androidx.fragment.app.Fragment
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class StringArgumentDelegate(private val name: String) : ReadOnlyProperty<Fragment, String?> {

	override fun getValue(thisRef: Fragment, property: KProperty<*>): String? {
		return thisRef.arguments?.getString(name)
	}
}