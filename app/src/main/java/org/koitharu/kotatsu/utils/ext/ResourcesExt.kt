package org.koitharu.kotatsu.utils.ext

import android.content.res.Resources
import androidx.annotation.Px
import kotlin.math.roundToInt

@Px
fun Resources.resolveDp(dp: Int) = (dp * displayMetrics.density).roundToInt()

@Px
fun Resources.resolveDp(dp: Float) = dp * displayMetrics.density