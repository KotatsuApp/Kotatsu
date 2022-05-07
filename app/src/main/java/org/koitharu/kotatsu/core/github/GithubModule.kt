package org.koitharu.kotatsu.core.github

import org.koin.dsl.module

val githubModule
	get() = module {
		factory {
			GithubRepository(get())
		}
	}