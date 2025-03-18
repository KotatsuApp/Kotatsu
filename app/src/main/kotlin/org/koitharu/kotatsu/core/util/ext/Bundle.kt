@file:Suppress("DEPRECATION")

package org.koitharu.kotatsu.core.util.ext

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import androidx.core.content.IntentCompat
import androidx.core.os.BundleCompat
import androidx.core.os.ParcelCompat
import androidx.lifecycle.SavedStateHandle
import org.koitharu.kotatsu.parsers.util.toArraySet
import java.io.Serializable
import java.util.EnumSet


// https://issuetracker.google.com/issues/240585930

inline fun <reified T : Parcelable> Bundle.getParcelableCompat(key: String): T? {
	return BundleCompat.getParcelable(this, key, T::class.java)
}

inline fun <reified T : Parcelable> Bundle.requireParcelable(key: String): T = checkNotNull(getParcelableCompat(key)) {
	"Parcelable of type \"${T::class.java.name}\" not found at \"$key\""
}

inline fun <reified T : Parcelable> Intent.getParcelableExtraCompat(key: String): T? {
	return IntentCompat.getParcelableExtra(this, key, T::class.java)
}

inline fun <reified T : Serializable> Intent.getSerializableExtraCompat(key: String): T? {
	return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
		getSerializableExtra(key, T::class.java)
	} else {
		getSerializableExtra(key) as T?
	}
}

inline fun <reified T : Serializable> Bundle.getSerializableCompat(key: String): T? {
	return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
		getSerializable(key, T::class.java)
	} else {
		getSerializable(key) as T?
	}
}

inline fun <reified T : Parcelable> Parcel.readParcelableCompat(): T? {
	return ParcelCompat.readParcelable(this, T::class.java.classLoader, T::class.java)
}

inline fun <reified T : Serializable> Parcel.readSerializableCompat(): T? {
	return ParcelCompat.readSerializable(this, T::class.java.classLoader, T::class.java)
}

inline fun <reified T : Serializable> Bundle.requireSerializable(key: String): T {
	return checkNotNull(getSerializableCompat(key)) {
		"Serializable of type \"${T::class.java.name}\" not found at \"$key\""
	}
}

fun <E : Enum<E>> Parcel.writeEnumSet(set: Set<E>?) {
	if (set == null) {
		writeValue(null)
	} else {
		val array = IntArray(set.size)
		set.forEachIndexed { i, e -> array[i] = e.ordinal }
		writeIntArray(array)
	}
}

inline fun <reified E : Enum<E>> Parcel.readEnumSet(): Set<E>? = readEnumSet(E::class.java)

fun <E : Enum<E>> Parcel.readEnumSet(cls: Class<E>): Set<E>? {
	val array = createIntArray() ?: return null
	if (array.isEmpty()) {
		return emptySet()
	}
	val enumValues = cls.enumConstants ?: return null
	val set = EnumSet.noneOf(cls)
	array.forEach { e ->
		set.add(enumValues[e])
	}
	return set
}

fun Parcel.writeStringSet(set: Set<String>?) {
	writeStringArray(set?.toTypedArray().orEmpty())
}

fun Parcel.readStringSet(): Set<String> {
	return this.createStringArray()?.toArraySet().orEmpty()
}

fun <T> SavedStateHandle.require(key: String): T {
	return checkNotNull(get(key)) {
		"Value $key not found in SavedStateHandle or has a wrong type"
	}
}

fun Parcelable.marshall(): ByteArray {
	val parcel = Parcel.obtain()
	return try {
		this.writeToParcel(parcel, 0)
		parcel.marshall()
	} finally {
		parcel.recycle()
	}
}

fun <T : Parcelable> Parcelable.Creator<T>.unmarshall(bytes: ByteArray): T {
	val parcel = Parcel.obtain()
	return try {
		parcel.unmarshall(bytes, 0, bytes.size)
		parcel.setDataPosition(0)
		createFromParcel(parcel)
	} finally {
		parcel.recycle()
	}
}

inline fun buildBundle(capacity: Int, block: Bundle.() -> Unit): Bundle = Bundle(capacity).apply(block)
