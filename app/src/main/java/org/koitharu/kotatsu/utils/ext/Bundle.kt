@file:Suppress("DEPRECATION")

package org.koitharu.kotatsu.utils.ext

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import org.koitharu.kotatsu.core.model.parcelable.ParcelableMangaTags
import java.io.Serializable

// https://issuetracker.google.com/issues/240585930

inline fun <reified T : Parcelable> Bundle.getParcelableCompat(key: String): T? {
	return getParcelable(key) as T?
}

inline fun <reified T : Parcelable> Intent.getParcelableExtraCompat(key: String): T? {
	return getParcelableExtra(key) as T?
}

inline fun <reified T : Serializable> Bundle.getSerializableCompat(key: String): T? {
	return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
		getSerializable(key, T::class.java)
	} else {
		getSerializable(key) as T?
	}
}

inline fun <reified T : Parcelable> Parcel.readParcelableCompat(): T? {
	return readParcelable(ParcelableMangaTags::class.java.classLoader) as T?
}

inline fun <reified T : Serializable> Parcel.readSerializableCompat(): T? {
	return readSerializable() as T?
}

inline fun <reified T : Serializable> Bundle.requireSerializable(key: String): T {
	return checkNotNull(getSerializableCompat(key)) {
		"Serializable of type \"${T::class.java.name}\" not found at \"$key\""
	}
}

inline fun <reified T : Parcelable> Bundle.requireParcelable(key: String): T {
	return checkNotNull(getParcelableCompat(key)) {
		"Parcelable of type \"${T::class.java.name}\" not found at \"$key\""
	}
}

fun <T> SavedStateHandle.require(key: String): T {
	return checkNotNull(get(key)) {
		"Value $key not found in SavedStateHandle or has a wrong type"
	}
}
