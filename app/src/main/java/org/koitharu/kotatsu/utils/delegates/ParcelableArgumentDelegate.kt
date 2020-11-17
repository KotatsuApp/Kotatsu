package org.koitharu.kotatsu.utils.delegates

import android.os.Parcelable
import androidx.fragment.app.Fragment
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class ParcelableArgumentDelegate<T : Parcelable>(private val name: String) :
	ReadOnlyProperty<Fragment, T> {

	override fun getValue(thisRef: Fragment, property: KProperty<*>): T {
		return thisRef.requireArguments().getParcelable(name)!!
	}
}