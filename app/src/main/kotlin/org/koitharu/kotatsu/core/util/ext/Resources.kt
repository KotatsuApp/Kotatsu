package org.koitharu.kotatsu.core.util.ext

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import androidx.annotation.Px
import kotlin.math.roundToInt

@Px
fun Resources.resolveDp(dp: Int) = (dp * displayMetrics.density).roundToInt()

@Px
fun Resources.resolveDp(dp: Float) = dp * displayMetrics.density

@SuppressLint("DiscouragedApi")
fun Context.getSystemBoolean(resName: String, fallback: Boolean): Boolean {
	val id = Resources.getSystem().getIdentifier(resName, "bool", "android")
	return if (id != 0) {
		createPackageContext("android", 0).resources.getBoolean(id)
	} else {
		fallback
	}
}
