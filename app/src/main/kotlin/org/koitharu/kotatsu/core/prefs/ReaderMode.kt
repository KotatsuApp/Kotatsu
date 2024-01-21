package org.koitharu.kotatsu.core.prefs

enum class ReaderMode(val id: Int) {

	STANDARD(1),
	REVERSED(3),
	VERTICAL(4),
	WEBTOON(2),
	;

	companion object {

		fun valueOf(id: Int) = entries.firstOrNull { it.id == id }
	}
}
