package org.koitharu.kotatsu.bookmarks

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import org.koitharu.kotatsu.bookmarks.domain.BookmarksRepository
import org.koitharu.kotatsu.bookmarks.ui.BookmarksViewModel

val bookmarksModule
	get() = module {

		factory { BookmarksRepository(get()) }

		viewModel { BookmarksViewModel(get()) }
	}