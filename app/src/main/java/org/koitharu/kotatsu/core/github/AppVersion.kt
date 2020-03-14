package org.koitharu.kotatsu.core.github

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class AppVersion(
	val id: Long,
	val name: String,
	val url: String,
	val apkSize: Long,
	val apkUrl: String
) : Parcelable {

	fun isGreaterThen(version: String) {
		val thisParts = name.substringBeforeLast('-').split('.')
		val parts = version.substringBeforeLast('-').split('.')
	}
}