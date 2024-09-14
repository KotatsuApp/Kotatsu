package org.koitharu.kotatsu.core.prefs

import androidx.annotation.Keep

@Keep
enum class ProgressIndicatorMode {

	NONE, PERCENT_READ, PERCENT_LEFT, CHAPTERS_READ, CHAPTERS_LEFT;
}
