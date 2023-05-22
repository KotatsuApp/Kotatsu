package org.koitharu.kotatsu.core.github

import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class AppVersion(
	val id: Long,
	val name: String,
	val url: String,
	val apkSize: Long,
	val apkUrl: String,
	val description: String,
) : Parcelable {

	@IgnoredOnParcel
	val versionId = VersionId(name)
}
