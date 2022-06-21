package org.koitharu.kotatsu.bookmarks

import org.koin.dsl.module
import org.koitharu.kotatsu.bookmarks.domain.BookmarksRepository

val bookmarksModule
	get() = module {

		factory { BookmarksRepository(get()) }
	}