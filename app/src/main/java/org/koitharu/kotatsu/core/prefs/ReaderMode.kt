package org.koitharu.kotatsu.core.prefs

enum class ReaderMode(val id: Int) {

	UNKNOWN(0),
	STANDARD(1),
	WEBTOON(2);

	companion object {

		fun valueOf(id: Int) = values().firstOrNull { it.id == id }
	}
}