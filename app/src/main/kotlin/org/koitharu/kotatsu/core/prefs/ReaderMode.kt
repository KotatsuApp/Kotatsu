package org.koitharu.kotatsu.core.prefs

enum class ReaderMode(val id: Int) {

	STANDARD(1),
	WEBTOON(2),
	REVERSED(3);

	companion object {

		fun valueOf(id: Int) = values().firstOrNull { it.id == id }
	}
}