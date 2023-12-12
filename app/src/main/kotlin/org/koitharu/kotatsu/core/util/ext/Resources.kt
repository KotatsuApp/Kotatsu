package org.koitharu.kotatsu.core.util.ext

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import androidx.annotation.Px
import androidx.core.util.TypedValueCompat
import kotlin.math.roundToInt

@Px
fun Resources.resolveDp(dp: Int) = resolveDp(dp.toFloat()).roundToInt()

@Px
fun Resources.resolveDp(dp: Float) = TypedValueCompat.dpToPx(dp, displayMetrics)

@Px
fun Resources.resolveSp(sp: Float) = TypedValueCompat.spToPx(sp, displayMetrics)

@SuppressLint("DiscouragedApi")
fun Context.getSystemBoolean(resName: String, fallback: Boolean): Boolean {
	val id = Resources.getSystem().getIdentifier(resName, "bool", "android")
	return if (id != 0) {
		createPackageContext("android", 0).resources.getBoolean(id)
	} else {
		fallback
	}
}
