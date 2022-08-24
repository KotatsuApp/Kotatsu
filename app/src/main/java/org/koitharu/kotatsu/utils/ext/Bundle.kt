package org.koitharu.kotatsu.utils.ext

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import java.io.Serializable

@Suppress("DEPRECATION")
inline fun <reified T : Parcelable> Bundle.getParcelableCompat(key: String): T? {
	return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
		getParcelable(key, T::class.java)
	} else {
		getParcelable(key) as? T
	}
}

@Suppress("DEPRECATION")
inline fun <reified T : Parcelable> Intent.getParcelableExtraCompat(key: String): T? {
	return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
		getParcelableExtra(key, T::class.java)
	} else {
		getParcelableExtra(key) as? T
	}
}

@Suppress("DEPRECATION")
inline fun <reified T : Serializable> Bundle.getSerializableCompat(key: String): T? {
	return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
		getSerializable(key, T::class.java)
	} else {
		getSerializable(key) as? T
	}
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
