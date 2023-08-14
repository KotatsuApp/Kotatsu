package org.koitharu.kotatsu.core.backup

import org.json.JSONArray

class BackupEntry(
	val name: String,
	val data: JSONArray
) {

	companion object Names {

		const val INDEX = "index"
		const val HISTORY = "history"
		const val CATEGORIES = "categories"
		const val FAVOURITES = "favourites"
		const val SETTINGS = "settings"
		const val BOOKMARKS = "bookmarks"
	}
}
