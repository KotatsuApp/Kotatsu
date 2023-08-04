package org.koitharu.kotatsu.core.prefs

import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import com.google.android.material.color.DynamicColors
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.parsers.util.find

enum class ColorScheme(
	@StyleRes val styleResId: Int,
	@StringRes val titleResId: Int,
) {

	DEFAULT(R.style.Theme_Kotatsu, R.string.system_default),
	MONET(R.style.Theme_Kotatsu_Monet, R.string.theme_name_dynamic),
	MIKU(R.style.Theme_Kotatsu_Miku, R.string.theme_name_miku),
	RENA(R.style.Theme_Kotatsu_Asuka, R.string.theme_name_asuka),
	FROG(R.style.Theme_Kotatsu_Mion, R.string.theme_name_mion),
	BLUEBERRY(R.style.Theme_Kotatsu_Rikka, R.string.theme_name_rikka),
	NAME2(R.style.Theme_Kotatsu_Sakura, R.string.theme_name_sakura),
	MAMIMI(R.style.Theme_Kotatsu_Mamimi, R.string.theme_name_mamimi),
	KANADE(R.style.Theme_Kotatsu_Kanade, R.string.theme_name_kanade)
	;

	companion object {

		val default: ColorScheme
			get() = if (DynamicColors.isDynamicColorAvailable()) {
				MONET
			} else {
				DEFAULT
			}

		fun getAvailableList(): List<ColorScheme> {
			val list = ColorScheme.entries.toMutableList()
			if (!DynamicColors.isDynamicColorAvailable()) {
				list.remove(MONET)
			}
			return list
		}

		fun safeValueOf(name: String): ColorScheme? {
			return ColorScheme.entries.find(name)
		}
	}
}
