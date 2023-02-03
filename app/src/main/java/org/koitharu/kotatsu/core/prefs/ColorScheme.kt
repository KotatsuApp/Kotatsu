package org.koitharu.kotatsu.core.prefs

import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import com.google.android.material.color.DynamicColors
import org.koitharu.kotatsu.R

enum class ColorScheme(
	@StyleRes val styleResId: Int,
	@StringRes val titleResId: Int,
) {

	DEFAULT(R.style.Theme_Kotatsu, R.string.system_default),
	MONET(R.style.Theme_Kotatsu_Monet, R.string.theme_name_dynamic),
	MINT(R.style.Theme_Kotatsu_Mint, R.string.theme_name_mint),
	OCTOBER(R.style.Theme_Kotatsu_October, R.string.theme_name_october),
	;

	companion object {

		val default: ColorScheme
			get() = if (DynamicColors.isDynamicColorAvailable()) {
				MONET
			} else {
				DEFAULT
			}

		fun getAvailableList(): List<ColorScheme> {
			val list = enumValues<ColorScheme>().toMutableList()
			if (!DynamicColors.isDynamicColorAvailable()) {
				list.remove(MONET)
			}
			return list
		}

		fun safeValueOf(name: String): ColorScheme? {
			return enumValues<ColorScheme>().find { it.name == name }
		}
	}
}
