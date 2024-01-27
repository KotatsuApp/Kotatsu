package org.koitharu.kotatsu.reader.data

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import org.koitharu.kotatsu.core.util.ext.getEnumValue
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.putEnumValue
import org.koitharu.kotatsu.reader.domain.TapGridArea
import org.koitharu.kotatsu.reader.ui.tapgrid.TapAction
import javax.inject.Inject

class TapGridSettings @Inject constructor(@ApplicationContext context: Context) {

	private val prefs = context.getSharedPreferences("tap_grid", Context.MODE_PRIVATE)

	fun getTapAction(area: TapGridArea, isLongTap: Boolean): TapAction? {
		val key = getPrefKey(area, isLongTap)
		return if (!isLongTap && key !in prefs) {
			getDefaultTapAction(area)
		} else {
			prefs.getEnumValue(key, TapAction::class.java)
		}
	}

	fun setTapAction(area: TapGridArea, isLongTap: Boolean, action: TapAction?) {
		val key = getPrefKey(area, isLongTap)
		prefs.edit { putEnumValue(key, action) }
	}

	fun observe() = prefs.observe().flowOn(Dispatchers.IO)

	private fun getPrefKey(area: TapGridArea, isLongTap: Boolean): String = if (isLongTap) {
		area.name + "_long"
	} else {
		area.name
	}

	private fun getDefaultTapAction(area: TapGridArea): TapAction = when (area) {
		TapGridArea.TOP_LEFT,
		TapGridArea.TOP_CENTER,
		TapGridArea.CENTER_LEFT,
		TapGridArea.BOTTOM_LEFT -> TapAction.PAGE_PREV

		TapGridArea.CENTER -> TapAction.TOGGLE_UI
		TapGridArea.TOP_RIGHT,
		TapGridArea.CENTER_RIGHT,
		TapGridArea.BOTTOM_CENTER,
		TapGridArea.BOTTOM_RIGHT -> TapAction.PAGE_NEXT
	}
}
