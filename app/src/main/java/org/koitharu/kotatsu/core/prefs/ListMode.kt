package org.koitharu.kotatsu.core.prefs

enum class ListMode(val id: Int) {

	LIST(0),
	DETAILED_LIST(1),
	GRID(2);

	companion object {

		fun valueOf(id: Int) = values().firstOrNull { it.id == id }
	}
}